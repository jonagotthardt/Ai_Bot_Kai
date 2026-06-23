package com.jonasmp.ai.pipeline;

public class UnicodeNormalizer {
   public double detectUnicodeAttack(String text) {
      if (text != null && !text.isEmpty()) {
         int suspicious = 0;
         int total = text.length();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cp = text.codePointAt(i);
            if (cp == 8238 || cp == 8237 || cp == 8236 || cp == 8206 || cp == 8207 || cp == 8294 || cp == 8295 || cp == 8296 || cp == 8297) {
               suspicious += 5;
            }

            if (this.isCyrillicHomoglyph(c)) {
               suspicious += 2;
            }

            if (this.isGreekHomoglyph(c)) {
               suspicious += 2;
            }

            if (Character.getType(c) == 6) {
               int combo = 0;

               for (int j = i; j < Math.min(i + 5, text.length()) && Character.getType(text.charAt(j)) == 6; j++) {
                  combo++;
               }

               if (combo > 2) {
                  suspicious += combo;
               }
            }

            if (cp >= 65280 && cp <= 65519) {
               suspicious++;
            }

            if (this.isRareBlock(cp)) {
               suspicious++;
            }
         }

         double ratio = (double)suspicious / (double)Math.max(total, 1);
         return Math.min(ratio * 3.0, 1.0);
      } else {
         return 0.0;
      }
   }

   public String replaceHomoglyphs(String text) {
      if (text == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder(text.length());

         for (char c : text.toCharArray()) {
            sb.append(this.mapHomoglyph(c));
         }

         return sb.toString();
      }
   }

   private boolean isCyrillicHomoglyph(char c) {
      return switch (c) {
         case 'а', 'в', 'е', 'з', 'к', 'м', 'н', 'о', 'р', 'с', 'т', 'у', 'ф', 'х', 'ъ', 'ы', 'ѕ', 'і', 'ј', 'љ' -> true;
         default -> false;
      };
   }

   private boolean isGreekHomoglyph(char c) {
      return switch (c) {
         case 'α', 'ε', 'η', 'ι', 'ν', 'ο', 'ρ', 'σ', 'υ', 'χ', 'ω' -> true;
         default -> false;
      };
   }

   private boolean isRareBlock(int cp) {
      return cp >= 119808 && cp <= 120831 || cp >= 9312 && cp <= 9471 || cp >= 8448 && cp <= 8527;
   }

   private char mapHomoglyph(char c) {
      return switch (c) {
         case 'α' -> 'a';
         case 'ε' -> 'e';
         case 'ι' -> 'i';
         case 'ν' -> 'v';
         case 'ο' -> 'o';
         case 'ρ' -> 'p';
         case 'σ' -> 'o';
         case 'υ' -> 'u';
         case 'χ' -> 'x';
         case 'ω' -> 'w';
         case 'а' -> 'a';
         case 'в' -> 'v';
         case 'е' -> 'e';
         case 'з' -> 'z';
         case 'к' -> 'k';
         case 'м' -> 'm';
         case 'н' -> 'n';
         case 'о' -> 'o';
         case 'р' -> 'p';
         case 'с' -> 'c';
         case 'т' -> 't';
         case 'у' -> 'y';
         case 'ф' -> 'f';
         case 'х' -> 'x';
         case 'ѕ' -> 's';
         case 'і' -> 'i';
         case 'ј' -> 'j';
         case 'ａ' -> 'a';
         case 'ｂ' -> 'b';
         case 'ｃ' -> 'c';
         case 'ｄ' -> 'd';
         case 'ｅ' -> 'e';
         case 'ｆ' -> 'f';
         case 'ｇ' -> 'g';
         case 'ｈ' -> 'h';
         case 'ｉ' -> 'i';
         case 'ｊ' -> 'j';
         case 'ｋ' -> 'k';
         case 'ｌ' -> 'l';
         case 'ｍ' -> 'm';
         case 'ｎ' -> 'n';
         case 'ｏ' -> 'o';
         case 'ｐ' -> 'p';
         case 'ｑ' -> 'q';
         case 'ｒ' -> 'r';
         case 'ｓ' -> 's';
         case 'ｔ' -> 't';
         case 'ｕ' -> 'u';
         case 'ｖ' -> 'v';
         case 'ｗ' -> 'w';
         case 'ｘ' -> 'x';
         case 'ｙ' -> 'y';
         case 'ｚ' -> 'z';
         default -> c;
      };
   }
}
