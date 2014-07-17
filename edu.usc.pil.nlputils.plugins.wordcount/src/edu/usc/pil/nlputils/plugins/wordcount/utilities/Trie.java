package edu.usc.pil.nlputils.plugins.wordcount.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Trie {

	private class Node {
		Map<Character, Node> children;
		boolean isWord;
		List<Integer> categories;
		char character;
		
		public Node(char c, List<Integer> categories, boolean isWord){
			this.character = c;
			this.isWord = isWord;
			this.categories = categories;
			this.children = new HashMap<Character, Node>();
		}
		
		public String toString(){
			return Character.toString(character);		
		}
	}
	
	Node root;
	
	public Trie(){
		root = new Node(' ', null, false); /* Initialize root node with space character and no categories*/
	}
	
	public void insert(String word, List<Integer> categories){
		Node next = root.children.get(word.charAt(0)), prev = root;
		int i=0;
		
		while(next != null && i != word.length()-1){
			//System.out.println(i+"/"+word.length() +" "+ word.charAt(i));
			prev = next;
			next = next.children.get(word.charAt(i+1));
			//System.out.println("Prev - " + prev.character);
			//if (next!=null)
			//System.out.println("Next - " + next.character);
			i++;
		}
		
		/* If I reached end of the trie without reaching the end of the string, add the new nodes corresponding to this new string */
		if((i!=word.length()) || i==0){
			Node n;
			while( i != word.length()){
				n = new Node(word.charAt(i), null, false);
				prev.children.put(word.charAt(i), n);
				//System.out.println("Inserted - " + n.character);
				prev = n;
				i++;
			}
		} else {
			// The string is in the trie. mark the current node's isWord to true to mark it as a valid word.
			//System.out.println("Marking "+next.character+" as valid.");
			//next.isWord = true; - instead, just set prev = next as the remaining instructions outside the condition do the rest.
			prev = next;
		}
		
		prev.categories = categories;
		prev.isWord = true;
	}
	
	public void printTrie(){
		
		Stack<Node> s = new Stack<Node>();
		Node n;
		Iterator it;
		Map.Entry pair;
		
		s.push(root);
		/* Do a DFS on the tree */
		while(!s.empty()){
			n = s.pop();
			System.out.print(n.character);
			it = n.children.entrySet().iterator();
			while(it.hasNext()){
				pair = (Map.Entry)it.next();
				s.push((Node)pair.getValue());
			}
		}
		System.out.println();
	}
	
	public List<Integer> query(String word){
		
		Node prev = root, next = root.children.get(word.charAt(0));
		
		int i=1;
		while( next != null && i != word.length()){
			prev = next;
			next = next.children.get(word.charAt(i));
			i++;
		}
		
		if(i == word.length() && next != null){
			prev = next;
		}
	
		if(prev.children.get('*') != null){
			return prev.children.get('*').categories;
		}
		
		if (i == word.length() && next == null & prev.children.get("*")==null){
			return null;
		}
		
		if(i == word.length()){
			if(prev.isWord){
				return prev.categories;
			}
		}		
		
		/* If I came out alive, it means I didn't find the word - so return a null */
		return null;
		
	}
	
}
