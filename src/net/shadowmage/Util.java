package net.shadowmage;


public class Util {

   public static String getSubstring(String input, int begin, int end) {
      if(input != null && input.length() != 0) {
         if(begin >= input.length()) {
            return "";
         } else {
            if(end > input.length()) {
               end = input.length();
            }

            return input.substring(begin, end);
         }
      } else {
         return "";
      }
   }
}
