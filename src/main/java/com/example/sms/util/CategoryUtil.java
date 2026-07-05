package com.example.sms.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CategoryUtil {

    @Value("${category.level1.min:75}")
    private double level1Min;

    @Value("${category.level2.min:50}")
    private double level2Min;

    public String determineCategory(double averageMarks) {
        if (averageMarks >= level1Min) {
            return "Level 1";
        } else if (averageMarks >= level2Min) {
            return "Level 2";
        } else {
            return "Level 3";
        }
    }
}
