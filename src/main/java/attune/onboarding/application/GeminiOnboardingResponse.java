package attune.onboarding.application;

import java.util.List;

/**
 * Gemini 온보딩 분석 응답.
 * visibleTags          : 사용자에게 노출할 trouble_tag(문제 상황) 이름 목록 (5~7개)
 * visibleConditionTags : 사용자에게 노출할 condition_tag(감정·컨디션) 이름 목록 (5~7개)
 * treatmentGoals       : 개인화 치료 목표 4개
 */
public record GeminiOnboardingResponse(
        List<String> visibleTags,
        List<String> visibleConditionTags,
        List<GoalItem> treatmentGoals
) {
    /**
     * @param goal          치료 목표 문장 (자연스러운 한국어)
     * @param functionalArea 기능 영역 한국어 값 — 업무/학업 | 시간관리 | 생활관리 | 정서/관계
     */
    public record GoalItem(
            String goal,
            String functionalArea
    ) {}
}
