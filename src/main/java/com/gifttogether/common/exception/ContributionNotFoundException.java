package com.gifttogether.common.exception;

public class ContributionNotFoundException extends RuntimeException {

    public ContributionNotFoundException() {
        super("참여 내역을 찾을 수 없습니다.");
    }
}