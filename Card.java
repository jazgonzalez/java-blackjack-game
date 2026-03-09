package finalProject;

public class Card {
	String rank; // 2-9, T, J, Q, K, A
	String suit; // diamonds, clubs, etc
	boolean faceUp;
	
	// constructor that creates a card with a given rank and suit
	public Card(String rank, String suit) {
		this.rank = rank;
		this.suit = suit;
	}

	// return the numeric value of the card 
	public int getValue() {
		
		switch(this.rank) {
			case "2": return 2;
			case "3": return 3;
			case "4": return 4;
			case "5": return 5;
			case "6": return 6;
			case "7": return 7;
			case "8": return 8;
			case "9": return 9;
			case "T": return 10;
			case "J": return 10;
			case "Q": return 10;
			case "K": return 10;
			case "A": return 11;
			default: return 0;
		}
	}
	
	public String getSuit() {
		switch(this.suit) {
		case "Diamonds": return "D";
		case "Clubs": return "C";
		case "Hearts": return "H";
		case "Spades": return "S";
		default: return "";
		}	
	}
		
	// show the card as a string ("AHearts")
	public String fullName() { 
		return this.rank + this.suit;
	}
	
	// show the card as a string ("2-C") 
	// used for the card images
	public String toString() {
		return this.rank + "-" + this.getSuit();
	}
	
	// check whether the card is face up
	boolean isFaceUp() {
		return this.faceUp;
	}
	
	// turn the card face up
	public void flipUp() {
		this.faceUp = true;
	}
	
	// turn the card face down
	public void flipDown() {
		this.faceUp = false;
	}
	
	public String getImagePath() {
		return "/finalProject/cards/" + toString() + ".png";
	}
	
}
