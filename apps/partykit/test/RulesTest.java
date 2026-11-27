package factory.fun;
public class RulesTest{public static void main(String[] args){if(Rules.choices(" A\nB\nA\n ").length!=2)throw new AssertionError();try{Rules.choices("A");throw new AssertionError();}catch(IllegalArgumentException expected){}System.out.println("Rules tests passed");}}
