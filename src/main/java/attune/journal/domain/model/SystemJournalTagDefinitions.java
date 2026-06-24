package attune.journal.domain.model;

import java.util.List;

public final class SystemJournalTagDefinitions {

    public record Definition(
            JournalTagCategory category,
            String name,
            String tagType,
            boolean defaultVisible
    ) {}

    private static final List<Definition> ALL = List.of(
            condition("평온", "CALM", true),
            condition("차분함", "CALM", false),
            condition("에너지 많음", "UP", true),
            condition("들뜸", "UP", false),
            condition("말이 많아짐", "UP", false),
            condition("의욕 없음", "DOWN", true),
            condition("피곤함", "DOWN", false),
            condition("몸이 무거움", "DOWN", false),
            condition("불안함", "TIGHT", true),
            condition("초조함", "TIGHT", false),
            condition("예민함", "TIGHT", false),
            condition("머리 멍함", "FOGGY", true),
            condition("생각이 끊김", "FOGGY", false),
            condition("집중이 흐려짐", "FOGGY", false),

            sideEffect("식욕 저하", true),
            sideEffect("입마름", true),
            sideEffect("두통", true),
            sideEffect("메스꺼움", false),
            sideEffect("두근거림", false),
            sideEffect("손 떨림", false),
            sideEffect("불면", true),
            sideEffect("졸림", false),
            sideEffect("피로감", false),
            sideEffect("어지러움", false),
            sideEffect("불안 증가", false),
            sideEffect("예민함", false),
            sideEffect("기분 저하", false),

            trouble("딴생각", "INATTENTION", true),
            trouble("깜빡함", "INATTENTION", true),
            trouble("설명을 놓침", "INATTENTION", false),
            trouble("중요한 내용을 빠뜨림", "INATTENTION", false),
            trouble("집중이 끊김", "INATTENTION", false),
            trouble("미룸", "TIME_MANAGEMENT", true),
            trouble("시작이 어려움", "TIME_MANAGEMENT", false),
            trouble("지각함", "TIME_MANAGEMENT", false),
            trouble("예상보다 오래 걸림", "TIME_MANAGEMENT", false),
            trouble("우선순위 못 정함", "TIME_MANAGEMENT", false),
            trouble("급하게 처리", "IMPULSIVITY", true),
            trouble("확인 전 제출함", "IMPULSIVITY", false),
            trouble("감정적으로 반응함", "IMPULSIVITY", false),
            trouble("가만히 있기 어려움", "HYPERACTIVITY", false),
            trouble("말이 많아짐", "HYPERACTIVITY", false),
            trouble("숫자/단위 실수", "COGNITIVE_ERROR", false),
            trouble("날짜/일정 착각", "COGNITIVE_ERROR", false),
            trouble("항목 혼동", "COGNITIVE_ERROR", false),
            trouble("순서를 헷갈림", "COGNITIVE_ERROR", false)
    );

    private SystemJournalTagDefinitions() {}

    public static List<Definition> all() {
        return ALL;
    }

    public static List<Definition> troubleTags() {
        return ALL.stream()
                .filter(definition -> definition.category() == JournalTagCategory.TROUBLE)
                .toList();
    }

    public static List<Definition> conditionTags() {
        return ALL.stream()
                .filter(definition -> definition.category() == JournalTagCategory.CONDITION)
                .toList();
    }

    private static Definition condition(String name, String tagType, boolean defaultVisible) {
        return new Definition(JournalTagCategory.CONDITION, name, tagType, defaultVisible);
    }

    private static Definition sideEffect(String name, boolean defaultVisible) {
        return new Definition(JournalTagCategory.SIDE_EFFECT, name, "NONE", defaultVisible);
    }

    private static Definition trouble(String name, String tagType, boolean defaultVisible) {
        return new Definition(JournalTagCategory.TROUBLE, name, tagType, defaultVisible);
    }
}
