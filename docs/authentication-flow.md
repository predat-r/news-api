# News API Authentication Architecture

This document describes the authentication and authorization code currently in
the News API. The application supports three login paths:

1. Local username and password authentication.
2. Google OpenID Connect login.
3. GitHub OAuth 2.0 login.

Every successful login resolves to a local `ApiUser` and issues the same News
API JWT. Protected endpoints therefore do not need to know how the user logged
in.

## Complete Architecture

```mermaid
flowchart LR
    Client[Browser, Swagger, or Postman]

    subgraph LoginMethods[Login methods]
        Local[Local username and password]
        Google[Google OIDC]
        GitHub[GitHub OAuth 2.0]
    end

    subgraph LocalPath[Local authentication]
        UserLoader[ApiUserDetailsService]
        LocalHandler[JwtAuthenticationSuccessHandler]
    end

    subgraph OAuthPath[Shared OAuth authentication]
        OAuthHandler[OAuthJwtAuthenticationSuccessHandler]
        ProviderContract[OAuthAccountService]
        GoogleService[GoogleAccountService]
        GitHubService[GithubAccountService]
    end

    subgraph IdentityData[Local identity data]
        UserRepo[ApiUserRepository]
        IdentityRepo[OAuthIdentityRepository]
        UserTable[(api_user)]
        IdentityTable[(oauth_identity)]
    end

    subgraph TokenSystem[Unified JWT system]
        JwtService[JwtService.issueToken]
        Encoder[JwtEncoder and RSA private key]
        Token[News API JWT]
        Decoder[JwtDecoder and RSA public key]
    end

    Client --> Local
    Client --> Google
    Client --> GitHub
    Local --> UserLoader --> UserRepo --> UserTable
    Local --> LocalHandler
    Google --> OAuthHandler
    GitHub --> OAuthHandler
    OAuthHandler --> ProviderContract
    ProviderContract --> GoogleService
    ProviderContract --> GitHubService
    GoogleService --> UserRepo
    GoogleService --> IdentityRepo
    GitHubService --> UserRepo
    GitHubService --> IdentityRepo
    IdentityRepo --> IdentityTable
    IdentityTable --> UserTable
    LocalHandler --> JwtService
    OAuthHandler --> JwtService
    JwtService --> Encoder --> Token --> Client
    Client --> Decoder
```

Google and GitHub prove an external identity, but their access tokens are not
accepted by the News API. The application creates its own local JWT after the
external identity has been associated with an `ApiUser`.

## Package Responsibilities

```text
security
|-- SecurityConfig.java
|-- OpenApiConfig.java
|-- api_user
|   |-- ApiUser.java
|   |-- ApiUserDetailsService.java
|   |-- ApiUserRepository.java
|   `-- Role.java
|-- jwt
|   |-- JwtAuthenticationSuccessHandler.java
|   |-- JwtKeyConfig.java
|   |-- JwtResponse.java
|   |-- JwtService.java
|   `-- JwtTokenSettings.java
|-- oauth
|   |-- OAuthAccountService.java
|   |-- OAuthIdentity.java
|   |-- OAuthIdentityRepository.java
|   |-- OAuthJwtAuthenticationSuccessHandler.java
|   |-- OAuthProvider.java
|   |-- google
|   |   `-- GoogleAccountService.java
|   `-- github
|       `-- GithubAccountService.java
```

- Root configuration selects the authentication mechanisms and URL rules.
- `api_user` owns local accounts, roles, and password-based user loading.
- `jwt` owns RSA key loading, JWT issuance, validation, and the local-login
  response.
- `oauth` owns the provider contract, external identity persistence, provider
  selection, the shared OAuth success response, and provider subpackages.
- `oauth.google` and `oauth.github` understand provider-specific attributes and
  provision local accounts.

## Application Startup

At startup:

1. `application.yaml` reads `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`,
   `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, and
   `GITHUB_CLIENT_SECRET` from the environment.
2. Liquibase creates and seeds `news` and `api_user`, then creates
   `oauth_identity`, adds nullable unique email support, and permits null
   passwords for OAuth-only users.
3. Hibernate runs with `ddl-auto: validate`, so it validates the Liquibase
   schema instead of creating it.
4. `JwtKeyConfig.jwtKeyPair(...)` Base64-decodes the PKCS#8 private key and
   X.509 public key.
5. `JwtKeyConfig.jwtEncoder(...)` creates the RS256 token encoder.
6. `JwtKeyConfig.jwtDecoder(...)` creates the decoder and requires issuer
   `news-api`.
7. `SecurityConfig.securityFilterChain(...)` enables form login, OAuth login,
   JWT resource-server authentication, URL authorization, and stateless API
   security.

## Local Login

```mermaid
sequenceDiagram
    actor Client
    participant Security as Spring Security
    participant UserLoader as ApiUserDetailsService
    participant UserRepo as ApiUserRepository
    participant UserDB as api_user
    participant Handler as JwtAuthenticationSuccessHandler
    participant JWT as JwtService

    Client->>Security: POST /login with username and password
    Security->>UserLoader: loadUserByUsername(username)
    UserLoader->>UserRepo: findByUsername(username)
    UserRepo->>UserDB: Select account
    UserDB-->>UserRepo: ApiUser row
    UserRepo-->>UserLoader: ApiUser
    alt Account is missing or has no local password
        UserLoader-->>Security: UsernameNotFoundException
        Security-->>Client: Generic authentication failure
    else Local account has a password hash
        UserLoader-->>Security: UserDetails with password hash and role
        Security->>Security: Compare password with BCrypt
        alt Password is valid
            Security->>Handler: onAuthenticationSuccess
            Handler->>UserRepo: findByUsername(authentication.name)
            UserRepo-->>Handler: ApiUser
            Handler->>JWT: issueToken(apiUser)
            JWT-->>Handler: Signed News API JWT
            Handler-->>Client: JSON JwtResponse
        else Password is invalid
            Security-->>Client: Generic authentication failure
        end
    end
```

`ApiUserDetailsService.loadUserByUsername(...)` calls
`.roles(user.getRole().name())`. Spring converts `ADMIN`, `EDITOR`, or
`REPORTER` into `ROLE_ADMIN`, `ROLE_EDITOR`, or `ROLE_REPORTER`.

OAuth-only accounts have no local password. The service rejects those accounts
with the same generic `UsernameNotFoundException` used for an unknown username,
so form login neither accepts them nor reveals which kind of account exists.
Their Google or GitHub login flow is unaffected.

The local success handler reloads the complete `ApiUser`, issues a JWT, and
writes it directly as JSON instead of redirecting to another page.

## Shared OAuth Login

Spring sends both provider callbacks through the one handler configured by:

```text
SecurityConfig.oauth2Login(...)
    -> OAuthJwtAuthenticationSuccessHandler
```

```mermaid
sequenceDiagram
    actor Browser
    participant Spring as Spring Security OAuth Login
    participant Handler as OAuthJwtAuthenticationSuccessHandler
    participant Services as List of OAuthAccountService
    participant ProviderSvc as Selected provider service
    participant JWT as JwtService

    Browser->>Spring: Complete Google or GitHub authorization
    Spring->>Handler: OAuth2AuthenticationToken
    Handler->>Handler: Read registrationId
    Handler->>Services: Find service with matching provider registrationId
    Services-->>Handler: GoogleAccountService or GithubAccountService
    Handler->>ProviderSvc: findOrCreateUser(principal)
    ProviderSvc-->>Handler: Local ApiUser
    Handler->>JWT: issueToken(apiUser)
    JWT-->>Handler: Signed News API JWT
    Handler-->>Browser: JSON JwtResponse
```

`OAuthAccountService` is the provider strategy contract. Each implementation:

- reports its `OAuthProvider`;
- accepts Spring's common `OAuth2User` type;
- validates and reads its own provider attributes;
- returns a local `ApiUser`.

`OAuthProvider` stores both the database enum value and Spring registration ID:

| Enum value | Registration ID |
| --- | --- |
| `GOOGLE` | `google` |
| `GITHUB` | `github` |

The common handler compares the callback registration ID with
`OAuthProvider.registrationId`. Adding another provider requires another enum
value, client registration, and `OAuthAccountService` implementation; it does
not require another success handler.

## Google Login

```mermaid
sequenceDiagram
    actor Browser
    participant App as Spring Security
    participant Google
    participant Handler as Shared OAuth success handler
    participant AccountSvc as GoogleAccountService
    participant IdentityRepo as OAuthIdentityRepository
    participant UserRepo as ApiUserRepository
    participant DB as H2 database

    Browser->>App: GET /oauth2/authorization/google
    App-->>Browser: Redirect to Google
    Google-->>App: Authorization code callback
    App->>Google: Exchange code and validate OIDC response
    App->>Handler: OAuth2AuthenticationToken for google
    Handler->>AccountSvc: findOrCreateUser(OidcUser)
    AccountSvc->>AccountSvc: Require sub, email, and verified email
    AccountSvc->>IdentityRepo: Find GOOGLE plus sub
    IdentityRepo->>DB: Select identity and related ApiUser
    alt Returning Google identity
        DB-->>AccountSvc: OAuthIdentity and ApiUser
    else First Google login
        AccountSvc->>UserRepo: Check username and email conflicts
        AccountSvc->>DB: Insert ApiUser with REPORTER role
        AccountSvc->>DB: Insert GOOGLE OAuthIdentity
    end
    AccountSvc-->>Handler: ApiUser
```

Google-specific behavior:

- The principal must be an `OidcUser`.
- `sub` is the stable provider subject.
- Email must be present and `email_verified` must be true.
- Email is normalized to lowercase and becomes both local email and username.
- A first-time account receives `REPORTER` and has a null password.
- Existing username or email produces an account-conflict exception rather
  than silently linking accounts.

## GitHub Login

```mermaid
sequenceDiagram
    actor Browser
    participant App as Spring Security
    participant GitHub
    participant Handler as Shared OAuth success handler
    participant AccountSvc as GithubAccountService
    participant IdentityRepo as OAuthIdentityRepository
    participant UserRepo as ApiUserRepository
    participant DB as H2 database

    Browser->>App: GET /oauth2/authorization/github
    App-->>Browser: Redirect to GitHub
    GitHub-->>App: Authorization code callback
    App->>GitHub: Exchange code and load OAuth2User
    App->>Handler: OAuth2AuthenticationToken for github
    Handler->>AccountSvc: findOrCreateUser(OAuth2User)
    AccountSvc->>AccountSvc: Require id and login
    AccountSvc->>IdentityRepo: Find GITHUB plus id
    IdentityRepo->>DB: Select identity and related ApiUser
    alt Returning GitHub identity
        DB-->>AccountSvc: OAuthIdentity and ApiUser
    else First GitHub login
        AccountSvc->>UserRepo: Check username and optional email conflicts
        AccountSvc->>DB: Insert ApiUser with REPORTER role
        AccountSvc->>DB: Insert GITHUB OAuthIdentity
    end
    AccountSvc-->>Handler: ApiUser
```

GitHub-specific behavior:

- The numeric `id` attribute is converted to text and used as the stable
  provider subject.
- `login` is required and normalized to lowercase.
- The local username is `github:<login>`, which avoids collision with ordinary
  local usernames.
- Email is optional because GitHub can omit private email addresses from the
  standard user response.
- If email is present, it is normalized and checked for conflicts.
- A first-time account receives `REPORTER` and has a null password.

## Identity Provisioning and Linking

Both provider services run in a transaction. On first login they save:

1. One `ApiUser`.
2. One `OAuthIdentity` pointing to that user.

If the second insert fails, the first insert rolls back. Returning login looks
up `(provider, providerSubject)` first and reuses the related user.

`OAuthIdentity.apiUser` remains lazily mapped by default. For the returning
login query, `OAuthIdentityRepository.findByProviderAndProviderSubject(...)`
uses `@EntityGraph(attributePaths = "apiUser")` to load the related `ApiUser`
with the identity. This ensures that `JwtService` can read the username and
role after the provider service transaction has ended, without causing a
`LazyInitializationException`.

Automatic account linking is intentionally not implemented. If a new provider
identity has an email or username already used by another local account, login
is rejected. Matching an email alone is not treated as proof that two external
identities should control the same local account.

A future explicit linking flow should begin while the user is already
authenticated, complete the second provider's OAuth flow, and attach only the
new `OAuthIdentity` to the existing `ApiUser`.

## Database Model

```mermaid
erDiagram
    API_USER ||--o{ OAUTH_IDENTITY : owns

    API_USER {
        BIGINT user_id PK
        VARCHAR username UK
        VARCHAR password "nullable for OAuth users"
        VARCHAR email UK "nullable"
        VARCHAR role
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    OAUTH_IDENTITY {
        BIGINT identity_id PK
        BIGINT user_id FK
        VARCHAR provider
        VARCHAR provider_subject
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    NEWS {
        BIGINT news_id PK
        VARCHAR title
        VARCHAR details
        VARCHAR reported_by
        TIMESTAMP reported_at
        TIMESTAMP updated_at
    }
```

Liquibase enforces:

- unique `api_user.username`;
- unique non-null `api_user.email`;
- foreign key from `oauth_identity.user_id` to `api_user.user_id` with delete
  cascade;
- unique `(provider, provider_subject)` so an external account is stored once;
- unique `(user_id, provider)` so one local user has at most one identity from
  each provider.

There is no foreign key between `news.reported_by` and `api_user.username`.
News ownership is a logical string relationship.

## Unified JWT

All login paths converge on:

```text
JwtService.issueToken(ApiUser apiUser)
```

```mermaid
flowchart TD
    LocalUser[Password-backed ApiUser] --> Issue[JwtService.issueToken]
    GoogleUser[Google-backed ApiUser] --> Issue
    GitHubUser[GitHub-backed ApiUser] --> Issue
    Issue --> Claims[Build JWT claims]
    PrivateKey[RSA private key] --> Encoder[JwtEncoder]
    Claims --> Encoder
    Encoder --> Token[RS256 News API JWT]
    Token --> Response[JwtResponse]
    Response --> Client[Client stores accessToken]

    Token --> Header[Header with typ JWT and alg RS256]
    Token --> Payload[iss, sub, iat, exp, jti, roles]
    Token --> Signature[RSA signature]
```

The JWT contains:

| Claim | Meaning |
| --- | --- |
| `iss` | Fixed issuer `news-api` |
| `sub` | Local `ApiUser.username` |
| `iat` | Issuance time |
| `exp` | Expiration, 60 minutes after issuance |
| `jti` | Random UUID token identifier |
| `roles` | List such as `ROLE_REPORTER` |

The response shape is:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

The private RSA key signs tokens. The public RSA key verifies them. Google and
GitHub tokens cannot replace this JWT because the resource server expects the
News API signature and issuer.

## Bearer Request and Authorization

Protected requests send:

```http
Authorization: Bearer <accessToken>
```

```mermaid
sequenceDiagram
    actor Client
    participant Filter as BearerTokenAuthenticationFilter
    participant Decoder as JwtDecoder
    participant Converter as JwtAuthenticationConverter
    participant Context as SecurityContext
    participant Authz as Method authorization
    participant NewsSvc as NewsService
    participant NewsDB as news table

    Client->>Filter: Request with bearer JWT
    Filter->>Decoder: Decode and validate
    Decoder->>Decoder: Verify RSA signature, issuer, and time claims
    Decoder->>Converter: Valid JWT with roles claim
    Converter->>Converter: Read roles with no extra prefix
    Converter->>Context: Username and ROLE authorities
    Context->>Authz: Evaluate PreAuthorize expression
    alt Authorized
        Authz->>NewsSvc: Invoke operation
        NewsSvc->>NewsDB: Read or modify news
        NewsDB-->>Client: API response
    else Forbidden
        Authz-->>Client: 403 Forbidden
    end
```

`SecurityConfig.jwtAuthenticationConverter()` reads the `roles` claim and sets
an empty authority prefix because the values already include `ROLE_`.

URL-level rules currently make these paths public:

- `/swagger-ui/**`
- `/swagger-ui.html`
- `/v3/api-docs/**`
- `GET /api/v1/news`
- `GET /api/v1/news/**`

Every other path requires authentication. Method security then enforces:

| Operation | ADMIN | EDITOR | REPORTER | Anonymous |
| --- | --- | --- | --- | --- |
| List news | Yes | Yes | Yes | Yes |
| Get news by ID | Yes | Yes | Yes | Yes |
| Create news | Yes | Yes | Yes | No |
| Update news | Yes | Yes | Own news only | No |
| Delete news | Yes | Yes | Own news only | No |

For ownership, `NewsAuthorization.isOwner(...)` checks whether `news.reported_by`
equals `authentication.name`. `NewsService.createNews(...)` writes that same
authenticated name into `reportedBy`:

- local reporter example: `reporter1`;
- Google reporter example: normalized Google email;
- GitHub reporter example: `github:<login>`.

## Session and CSRF Behavior

The API security context is stateless and protected API requests authenticate
with bearer tokens. CSRF is disabled in the current filter chain.

OAuth login still needs temporary state between the initial provider redirect
and `/login/oauth2/code/{registrationId}`. Spring may use an HTTP session for
that authorization-request state even though the resulting API authentication
uses a stateless JWT. That temporary OAuth state is not the API login session.

## Development Database

The application uses `jdbc:h2:mem:newsdb`, so all data is lost when the
application process ends. The H2 console is enabled in `application.yaml` at
`/h2-console`.

The current `SecurityConfig` does not permit the H2 console path or relax frame
headers for it, so the browser console is not currently usable through the
secured application. Development-only console access requires a dedicated H2
security rule or filter chain. It should not be exposed in production.

## Current Review Notes

The intended package boundaries and main authentication flows are coherent and
the project compiles. The following limitations remain:

1. H2 console access is enabled in configuration but not permitted by the
   current security filter chain.
2. Existing endpoint tests predate JWT authentication and authorization. They
   need authenticated JWT identities plus role and ownership coverage.
3. JWTs are not stored or revoked. Logout means deleting the token client-side;
   a copied token remains valid until expiration.
4. Changing a role in the database does not alter an already-issued JWT. The
   new role takes effect after another token is issued.
5. Provider account creation performs check-then-insert operations. Database
   unique constraints preserve integrity during concurrent first logins, but a
   resulting constraint violation is not translated into a provider-friendly
   authentication response.
6. Account linking is not implemented. A second provider with an existing
   username or email is rejected rather than attached automatically.
7. Security and Liquibase DEBUG logging, the H2 console, and full actuator
   exposure are development settings and should be restricted in production.
