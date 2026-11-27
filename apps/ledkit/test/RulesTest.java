package factory.fun;
public class RulesTest{public static void main(String[] args){if(Rules.position(100,20,2,8)!=40||Rules.position(100,20,0,0)!=100||Rules.position(100,20,1,0)!=-20||Rules.position(100,20,0,120)!=100)throw new AssertionError();System.out.println("Rules tests passed");}}
