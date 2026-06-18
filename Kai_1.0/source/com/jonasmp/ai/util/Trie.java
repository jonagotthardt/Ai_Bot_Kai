package com.jonasmp.ai.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

public class Trie {
   private final Trie.TrieNode root = new Trie.TrieNode();
   private boolean built = false;

   public void insert(String word) {
      if (word != null && !word.isEmpty()) {
         Trie.TrieNode node = this.root;

         for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new Trie.TrieNode());
         }

         node.isEnd = true;
         node.word = word;
      }
   }

   public void build() {
      Queue<Trie.TrieNode> queue = new LinkedList<>();

      for (Trie.TrieNode child : this.root.children.values()) {
         child.fail = this.root;
         queue.add(child);
      }

      while (!queue.isEmpty()) {
         Trie.TrieNode current = queue.poll();

         for (Entry<Character, Trie.TrieNode> entry : current.children.entrySet()) {
            char ch = entry.getKey();
            Trie.TrieNode child2 = entry.getValue();
            Trie.TrieNode failNode = current.fail;

            while (failNode != null && !failNode.children.containsKey(ch)) {
               failNode = failNode.fail;
            }

            if (failNode == null) {
               child2.fail = this.root;
            } else {
               child2.fail = failNode.children.get(ch);
            }

            queue.add(child2);
         }
      }

      this.built = true;
   }

   public List<String> search(String text) {
      if (!this.built) {
         this.build();
      }

      List<String> matches = new ArrayList<>();
      Set<String> found = new HashSet<>();
      Trie.TrieNode node = this.root;

      for (char c : text.toCharArray()) {
         while (node != null && !node.children.containsKey(c)) {
            node = node.fail;
         }

         Trie.TrieNode temp;
         if (node == null) {
            node = this.root;
         } else {
            for (node = temp = node.children.get(c); temp != null && temp != this.root; temp = temp.fail) {
               if (temp.isEnd && !found.contains(temp.word)) {
                  found.add(temp.word);
                  matches.add(temp.word);
               }
            }
         }
      }

      return matches;
   }

   public boolean containsAny(String text) {
      return !this.search(text).isEmpty();
   }

   public int countMatches(String text) {
      return this.search(text).size();
   }

   private static class TrieNode {
      final Map<Character, Trie.TrieNode> children = new HashMap<>();
      Trie.TrieNode fail = null;
      boolean isEnd = false;
      String word = null;
   }
}
