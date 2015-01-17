package com.example.Demo_Subj;


import android.util.Log;

import java.util.*;


/**
 * Completely created by Michael on 02.12.2014.
 */
public class Intelligence {
    private int state = 0;      //Zustandvaribale für getNextAction; wird final nicht mehr benötigt

    private List<Need> needs_list = new ArrayList<Need>(); 		//verwaltet die Bedürfnisse als abstrakte Objekte ohne konkretem Namen als Vektor
    private int	randomStart;		    //Konstante: Startwert für Zufallszahlengenerator
    private int	randomEnd;		        //Konstante: Endwert für Zufallszahlengenerator
    private int	maxMotivationIndex;   //Der Index in der Liste, wo das Bedürfnis mit der höchsten Motivation steht


    //Standradkonstrktor - nimmt Werte, die sich als sinnvoll bzw logisch ergeben haben
    public Intelligence() {

        //Initialisierung des Vectors
        initVector();

        //Initialisierung privater Attribute mit eigens festgelegten Werten:
        initAttributes(1, 10); //Erzeugung von Zufallszahlen zwischen 1 und 10
    }

    //überladener Konstruktor - für die Tests, um die Intelligence verschieden zu konfigurieren
    public Intelligence(int randomStart, int randomEnd){
        //Initialisierung des Vectors
        initVector();

        //Initialisierung privater Attribute:
        initAttributes(randomStart, randomEnd);
    }

    //Liest die .xml und füllt den Vektor mit Bedürfnissen
    private void initVector(){
        needs_list.clear();
        needs_list.addAll(InformationPublisher.getNeedsFromXml("needs.xml"));
    }

    //Belegt die privaten Attribute mit Anfangswerten
    private void initAttributes(int randomStart, int randomEnd){
        int temp;       //zum Vertauschen von this.randomEnd und this.randomStart

        maxMotivationIndex = 0;

        if(this.randomStart < 0)                        //this.randomStart ist negativ -> * -1
            this.randomStart = this.randomStart * -1;

        if(this.randomEnd < 0)                          //this.randomEnd ist negativ -> * -1
           this.randomEnd = this.randomEnd * -1;

        if(this.randomStart > this.randomEnd){        //this.randomStart ist größer als this.randomEnd -> vertauschen
            temp = this.randomStart;
            this.randomStart = this.randomEnd;
            this.randomEnd = temp;
        }

        if(this.randomEnd == this.randomStart)        //this.randomStart ist identisch this.randomEnd -> this.randomEnd + 1
            this.randomEnd++;

        randomStart = this.randomStart;
        randomEnd = this.randomEnd;
    }

    //Aufruf von Subjekt; Gibt Objekt_ID zurück, zu der das Subjekt als nächstes laufen soll
    public Item getNextItem(){
        //Bedürfnis mit der höchsten Motivation resetten
        try {
            needs_list.get(maxMotivationIndex).setCurrentValue(0);
            return World.getItemById(needs_list.get(maxMotivationIndex).getObjectID());
        }
        catch(IndexOutOfBoundsException e){
            Log.e("LCP_Intelligence", "Index " + maxMotivationIndex + " does not exist in needs_list mit " + needs_list.size() + " Elementen.",e);

            return null;
        }
    }

    //um mit dem unfertigen Projekt kompilieren zu können, wird die Method noch beibehalten
    //fliegt aber raus, sobald die Schnittstelle und der Übergabewert geklärt sind
    public SubjectMoveAction getNextAction() {
        switch (state) {
            case 0:
                state = 1;
                return new SubjectMoveAction(680, 0);
            case 1:
                state = 0;
                return new SubjectMoveAction(40, 5);
        }
        return new SubjectMoveAction(0, 1);
    }

    //Algorithmus, in dem die Intelligence den Vektor verwaltet und alle Bedürfnisse neu berechnet
    public void tick() {
        boolean day = InternalClock.isDay();

        int motivation_highest = 0;
        Need beduerfnis;

        //Index der höchsten Motivation zurücksetzen auf 0;
        maxMotivationIndex = 0;

        //Liste inkrementieren
        for(int i=0; i < needs_list.size(); i++) {
            //bedürfnis aus Liste nehmen
            beduerfnis = needs_list.get(i);

            //Prüfen ob Tag oder Nacht ist und die entsprechenden Bedürfnisse nutzen
            if (day && beduerfnis.getActiveDayNight()) { //es ist Tag und Bedürfnis ist tagaktiv
                motivation_highest = manageList(i, motivation_highest);

            } else if (!day && !beduerfnis.getActiveDayNight()) { //es ist Nacht und das Bedürfnis ist nachtaktiv
                motivation_highest = manageList(i, motivation_highest);
            }
        }
    }

    //Zufallszahl zwischen randomStart und randomEnd erzeugen
    private int getIntRandom(){
        return randomStart + (int) Math.round( Math.random() * (randomEnd - randomStart) );
    }

    //Werte des Bedürfnisses an der Stelle need_list[index] werden geändert und motivation_highest neu berechnet
    private int manageList(int index, int motivation_highest){
        //es kann nicht das Objekt "beduerfnis" übergeben werden, da sich die eigenschfaten des Objektes ändern müssen. Dies soll dierekt in der Liste geschehen.

        try {
            //aktuellen Wert ändern; aktueller Wert = aktueller Wert + Zufallszahl
            needs_list.get(index).setCurrentValue(needs_list.get(index).getCurrentValue() + getIntRandom());

            //Motivation neu berechnen; Motivation = Priorität * (aktueller Wert - Schwellwert)
            needs_list.get(index).setMotivation(needs_list.get(index).getPriority() * (needs_list.get(index).getCurrentValue() - needs_list.get(index).getTopLevel()));
        }
        catch(ArrayIndexOutOfBoundsException e){ //Wird von get(index) geworfen, wenn dieser Index nicht im Vektor existiert.
            //Exception wird geloggt
            Log.e("LCP_Intelligence", "Index " + index + " does not exist in needs_list.",e);

            //Im Fehlerfall wird der übergeben Wert zurückgegeben
            return motivation_highest;
        }
        catch(Exception e){ // Andere Exceptions können auf Grund anderer Sicherheitsvorkehrungen nicht auftreten. Werden hier für den unvorhergesehenen Fall dennoch abgefangen.
            //Exception wird geloggt
            Log.e("LCP_Intelligence", "Exception while managing needs_list.",e);

            //Im Fehlerfall wird der übergeben Wert zurückgegeben
            return motivation_highest;
        }
        catch(Error e){     //irgendwo ist ein (gravierender) Fehler aufgetreten
            //Error wird geloggt
            Log.e("LCP_Intelligence", "Error while managing needs_list.",e);

            //Im Fehlerfall wird der übergeben Wert zurückgegeben
            return motivation_highest;
        }

        //Höchte Motivation finden:
        //neuer index, wenn neue höchste Motivation
        if(needs_list.get(index).getMotivation() > motivation_highest){
            maxMotivationIndex = index;
            motivation_highest = needs_list.get(index).getMotivation();
            //neuer Index, wenn Motivationen übereinstimmen UND Priorität des neuen Bedürfnisses größer ist der Priorität des alten Bedürfnisses
        }else if(needs_list.get(index).getMotivation() == motivation_highest && (needs_list.get(index).getPriority() > needs_list.get(maxMotivationIndex).getPriority())){
            maxMotivationIndex = index;
            motivation_highest = needs_list.get(index).getMotivation();
        }

        return motivation_highest;
    }

    //KI übernimmt Touchevents
    public void getTouchEvent(int ItemID){
        int rnd;

        for(int i=0; i < needs_list.size();i++){
            if(needs_list.get(i).getObjectID() == ItemID) {

                rnd = needs_list.get(i).getCurrentValue() + (int) Math.round( Math.random() * ((randomEnd+100) - (randomStart) ));  //Zufallszahl Erzuegen; sie größer ist, als alle, die sonst als Inkrement dienen (siehe Methode getIntRandom())

                try {
                    //aktueller_Wert = aktueller_Wert + (neue, größere) Zufallszahl
                    needs_list.get(i).setCurrentValue(needs_list.get(i).getCurrentValue() + rnd);

                    //Motivation neu berechnen; Motivation = Priorität * (aktueller Wert - Schwellwert)
                    needs_list.get(i).setMotivation(needs_list.get(i).getPriority() * (needs_list.get(i).getCurrentValue() - needs_list.get(i).getTopLevel()));

                    Log.d("LCP_Intelligence", "ObjectID " + ItemID + ". Neuer aktueller Wert: " + needs_list.get(i).getCurrentValue() + ". Inkrementiert durch: " + rnd + ". Ergibt neue Motivation: " + needs_list.get(i).getMotivation());
                } catch (ArrayIndexOutOfBoundsException e) { //Wird von get(index) geworfen, wenn dieser Index nicht im Vektor existiert.
                    //Exception wird geloggt
                    Log.e("LCP_Intelligence", "Index " + i + " does not exist in needs_list.", e);

                } catch (Exception e) { // Andere Exceptions können auf Grund anderer Sicherheitsvorkehrungen nicht auftreten. Werden hier für den unvorhergesehenen Fall dennoch abgefangen.
                    //Exception wird geloggt
                    Log.e("LCP_Intelligence", "Exception while TouchEvents in Intelligence.", e);

                } catch (Error e) {     //irgendwo ist ein (gravierender) Fehler aufgetreten
                    //Error wird geloggt
                    Log.e("LCP_Intelligence", "Error while TouchEvent in Intelligence.", e);
                }
            }
        }

        //Note: TopLevel, Priority oder Motivation eines Bedürfnisses können/ dürfen nicht verändert werden, da diese Änderungen nicht rückgängig gemacht werden können.
        //      Bei den TouchEvents handelt es sich aber nur im temporäre Ereignisse. Daher wird nur der aktuelle Wert und dem entsprechewnd die Motivation deutlich mehr als sonst hochgesetzt.
    }


}