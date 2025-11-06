package kr.go.namwon.seniorcenter.app.util;

public class StringUtil {

    public static String formatPhoneNumber(String input) {
        // 숫자만 남기기 (공백, 하이픈 제거)
        input = input.replaceAll("[^0-9]", "");

        // 너무 짧은 경우 그대로 반환
        if (input.length() < 3) return input;

        // 02 지역번호 (서울)인 경우 예외 처리
        if (input.startsWith("02")) {
            if (input.length() == 9) { // 예: 0212341234 → 02-1234-1234
                return input.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "$1-$2-$3");
            } else if (input.length() == 10) { // 예: 02123451234
                return input.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "$1-$2-$3");
            } else if (input.length() == 8) { // 예: 02123456
                return input.replaceFirst("(\\d{2})(\\d{3})(\\d{3})", "$1-$2-$3");
            }
        }

        // 휴대폰 (010, 011, 016, 017, 018, 019)
        if (input.matches("^01[016789]\\d{7,8}$")) {
            return input.replaceFirst("(^01[016789])(\\d{3,4})(\\d{4})", "$1-$2-$3");
        }

        // 일반 전화번호 (3자리 지역번호)
        if (input.matches("^(0\\d{2})(\\d{3,4})(\\d{4})$")) {
            return input.replaceFirst("^(0\\d{2})(\\d{3,4})(\\d{4})$", "$1-$2-$3");
        }

        // 기타 (매칭 안 되면 그대로 반환)
        return input;
    }

}
