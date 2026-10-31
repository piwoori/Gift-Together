package com.gifttogether.common.exception;

public class WishlistItemNotFoundException extends RuntimeException {

    public WishlistItemNotFoundException() {
        super("위시리스트 항목을 찾을 수 없습니다.");
    }
}