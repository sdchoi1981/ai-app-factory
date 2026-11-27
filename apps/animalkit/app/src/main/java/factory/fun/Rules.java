package factory.fun;
final class Rules{
static int nextQuestion(int previous,int count,java.util.Random random){
 if(count<2||previous< -1||previous>=count)throw new IllegalArgumentException();
 if(previous<0)return random.nextInt(count);
 int next=random.nextInt(count-1);
 return next>=previous?next+1:next;
}
static int[] options(int correct,int count,java.util.Random random){if(count<3||correct<0||correct>=count)throw new IllegalArgumentException();java.util.ArrayList<Integer> a=new java.util.ArrayList<>();for(int i=0;i<count;i++)if(i!=correct)a.add(i);java.util.Collections.shuffle(a,random);java.util.ArrayList<Integer> b=new java.util.ArrayList<>(a.subList(0,2));b.add(correct);java.util.Collections.shuffle(b,random);return new int[]{b.get(0),b.get(1),b.get(2)};}}
