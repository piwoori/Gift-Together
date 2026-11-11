/**
 * 서버에서 전달한 펀딩 마감일을 밀리초 타임스탬프로 변환합니다.
 *
 * 시간대 정보가 없는 날짜는 한국 시간(Asia/Seoul, UTC+09:00)으로
 * 해석하고, 시간대 정보가 있다면 해당 정보를 그대로 사용합니다.
 */
export function getDeadlineTime(expiredAt: string): number {
    const hasTimezone = /(?:Z|[+-]\d{2}:\d{2})$/i.test(expiredAt);

    const dateString = hasTimezone
        ? expiredAt
        : `${expiredAt}+09:00`;

    return new Date(dateString).getTime();
}

/**
 * 마감일이 유효하고 아직 지나지 않았는지 확인합니다.
 */
export function isFundingBeforeDeadline(
    expiredAt: string,
    now: number
): boolean {
    const deadline = getDeadlineTime(expiredAt);

    return Number.isFinite(deadline) && now < deadline;
}
