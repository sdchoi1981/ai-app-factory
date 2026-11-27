package factory.fun;
public class RulesTest{public static void main(String[] args){if(Rules.bound(-1)!=0||Rules.bound(10000)!=9999||Rules.bound(10)!=10)throw new AssertionError();System.out.println("Rules tests passed");}}
