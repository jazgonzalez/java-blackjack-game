package finalProject;
import java.util.ArrayList;


// COMPOSITION CLASS: Hand to be used by the Player (parent class)
class Hand
{
	private ArrayList<Card> cards; //list of cards
	
	//constructor
	public Hand() 
	{
		this.cards = new ArrayList<>(); // Initialize the list
	}
	
	//method to add cards to the list
	public void add(Card c)
	{
		cards.add(c);
	}
	
	//method to show many cards are in the list
	public int size()
	{
		return cards.size();
		
	}
	
	//remove all cards from list when restarting the game
	public void clear()
	{
		cards.clear();
	}
	
	public Card get(int index) 
	{  
	    return cards.get(index);
	}
	
    // Logic for calculating the score
	// En Player.java, dentro de la clase Hand

	public int calculateScore() 
	{
	    int total = 0;
	    int aces = 0; 

	    for (Card c : cards) 
	    {
	        int val = c.getValue(); // Obtiene 11 para A, 10 para K,Q,J
	        
	        if (val == 11) 
	        {
	            aces++;   // Si vale 11, es un As
	        }
	        
	        total += val; // Sumamos el valor
	    }

	    // Si nos pasamos de 21 y tenemos Ases contados como 11,
	    // los convertimos en 1
	    while (total > 21 && aces > 0) 
	    { 
	        total -= 10; // Convertir As de 11 a 1
	        aces--; 
	    }
	    
	    return total; 
	}
	
}

//-------------------PARENT CLASS-------------------
//used for dealer and human players 
public class Player 
{
	protected Hand hand; //allows child to access only if neccesary 
	private String name; //name of the player
	
	//constructor
	public Player()
	{
		this.hand = new Hand(); //create an object
		this.name = "Player"; //assign its default name
	}
	
	//overloaded constructor: to provide a specific name to the player
	public Player(String name)
	{
		this.hand = new Hand(); //create an object
		this.name = name; //assign its default name
	}	
	
	public void addCard(Card c)
	{
		hand.add(c);
	}
	
	//method to get the score (calculated in hand class)
	public int getScore()
	{
		return hand.calculateScore();
	}
	
	//getter: returns how many cards the palyer has
	public int getHandSize()
	{
		return hand.size();
	}
	
	//returns a specific card
	public Card getCard(int index)
	{
		return hand.get(index);
	}
	
	//reset the hand for a new round
	public void resetHand()
	{
		hand.clear();
	}
	
}
