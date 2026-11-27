package factory.fun;

import java.util.Arrays;
import java.util.Random;

public class RulesTest {
    public static void main(String[] args) {
        for (int seed = 0; seed < 100; seed++) {
            Random random = new Random(seed);
            for (int previous = -1; previous < 12; previous++) {
                int question = Rules.nextQuestion(previous, 12, random);
                if (question < 0 || question >= 12 || question == previous) throw new AssertionError();
                int[] options = Rules.options(question, 12, random);
                if (options.length != 3 || Arrays.stream(options).distinct().count() != 3
                        || Arrays.stream(options).noneMatch(i -> i == question)
                        || Arrays.stream(options).anyMatch(i -> i < 0 || i >= 12)) {
                    throw new AssertionError("Every quiz needs a unique correct answer");
                }
            }
        }
        System.out.println("Quiz selection, answer uniqueness and repeat avoidance verified");
    }
}
