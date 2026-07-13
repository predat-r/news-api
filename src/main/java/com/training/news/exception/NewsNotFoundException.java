package com.training.news.exception;

public class NewsNotFoundException extends RuntimeException {

	public NewsNotFoundException(Long newsId) {
		super("News not found with id: " + newsId);
	}
}
