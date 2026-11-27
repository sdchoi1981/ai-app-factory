package factory.fun;
public class RulesTest{public static void main(String[] args){for(int n=0;n<100;n++){int[] a=Rules.options(3,12,new java.util.Random(n));if(a.length!=3||java.util.Arrays.stream(a).distinct().count()!=3||java.util.Arrays.stream(a).noneMatch(i->i==3)||java.util.Arrays.stream(a).anyMatch(i->i<0||i>=12))throw new AssertionError();}System.out.println("Rules tests passed");}}
