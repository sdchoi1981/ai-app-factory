package factory.fun;
final class Rules{static String[] choices(String input){java.util.LinkedHashSet<String> set=new java.util.LinkedHashSet<>();for(String line:input.split("\\R")){String s=line.trim();if(!s.isEmpty())set.add(s);}if(set.size()<2||set.size()>30)throw new IllegalArgumentException();return set.toArray(new String[0]);}}
