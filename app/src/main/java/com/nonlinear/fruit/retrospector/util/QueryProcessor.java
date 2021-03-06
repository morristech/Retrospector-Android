package com.nonlinear.fruit.retrospector.util;

/**
 * Created by nonfrt on 9/30/17.
 */

import com.nonlinear.fruit.retrospector.model.Media;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class QueryProcessor {

    public static enum Operator {
        CONTAINS('~'), LESS_THAN('<'), GREATER_THAN('>'), EQUAL('=');
        public final char symbol;
        Operator(char symbol){ this.symbol = symbol; }
        @Override
        public String toString(){ return ""+symbol; }
        public static Operator fromChar(char x){
            for (Operator op : Operator.values())
                if (op.symbol == x)
                    return op;
            return null;
        }
    }

    public static enum Logic {
        AND(':'), OR('|'), NOT('!'), CMD('`');
        public final char symbol;
        Logic(char symbol){ this.symbol = symbol; }
        @Override
        public String toString(){ return ""+symbol; }
        public static Logic fromChar(char x){
            for (Logic op : Logic.values())
                if (op.symbol == x)
                    return op;
            return null;
        }
    }

    public static enum Command {
        TITLE('T'), CREATOR('C'), SEASON('S'), EPISODE('E'),
        CATEGORY('A'), FACTIOD('F'), RATING('#'), USER('U'), DATE('D');
        public final char symbol;
        Command(char symbol){ this.symbol = symbol; }
        @Override
        public String toString(){ return ""+symbol; }
        public static Command fromChar(char x){
            for (Command op : Command.values())
                if (op.symbol == x)
                    return op;
            return null;
        }
    }

    public static String toCheatsheet(){
        return "";
    }

    private String query;
    private String description;
    private List<Media> allMedia;
    QueryProcessor(String query, List<Media> media){
        this.query = query;
        this.allMedia = media;
    }

    private static String cleanSearchable(String searchable) {
        return searchable.toLowerCase();
    }

    private static List<String> toSearchable(Media media) {
        List<String> searchables = new ArrayList<>();
        searchables.addAll(Arrays.asList(
                cleanSearchable(media.getTitle()),
                cleanSearchable(media.getCreator()),
                cleanSearchable(media.getSeason()),
                cleanSearchable(media.getEpisode()),
                cleanSearchable(media.getCategory())
        ));
        return searchables;
    }

    public static boolean isMatchForMedia(String query, Media media) {
        boolean passAND = true;
        if(
                query.endsWith(""+Logic.AND) ||
                        query.endsWith(""+Logic.OR)  ||
                        query.endsWith(""+Logic.NOT)
                )
            query = query.substring(0, query.length()-1);

        // Split on AND first
        String[] queries = query.split(Pattern.quote(""+Logic.AND));
        List<String> searchables = toSearchable(media);
        for (String q : queries) {
            // Split on OR second
            String[] optns = q.split(Pattern.quote(""+Logic.OR));
            boolean passOR = false;
            for (String optn : optns) {
                boolean hasNegator = optn.length()>1 && optn.startsWith(""+Logic.NOT);
                boolean hasCmd = optn.length()>0 && optn.startsWith(""+Logic.CMD);
                boolean hasCleanOptn = false;
                boolean hasSubOptn = false;
                for (String s : searchables) {
                    if (s.contains(cleanSearchable(optn)))
                       hasCleanOptn = true;
                    if (s.contains(optn.substring(1)))
                        hasSubOptn = true;
                }
                if ( !hasNegator &&
                        !hasCmd  &&
                        hasCleanOptn )
                    passOR = true;
                else if ( hasNegator && !hasSubOptn )
                    passOR = true;
                else if ( hasCmd && isMatchForCmd(optn,media) )
                    passOR = true;
            }
            if (!passOR) {
                passAND = false;
            }
        }
        return passAND;
    }

    private static boolean isMatchForCmd(String cmd, Media media) {
        // Get rid of CMD op
        if( cmd.startsWith(""+Logic.CMD))
            cmd = cmd.substring(1, cmd.length());
        // Make sure cmd has a Command, Operator, and at least 1 character
        if( cmd.length() < 3)
            return true;

        char command = cmd.charAt(0);
        char operator = cmd.charAt(1);
        String valueLookedFor = cmd.substring(2,cmd.length());

        // Find the value
        String valueFound;
        switch(Command.fromChar(command)) {
            case TITLE:
                valueFound = media.getTitle();
                break;
            case CREATOR:
                valueFound = media.getCreator();
                break;
            case SEASON:
                valueFound = media.getSeason();
                break;
            case EPISODE:
                valueFound = media.getEpisode();
                break;
            case CATEGORY:
                valueFound = media.getCategory();
                break;
            default:
                valueFound = "";
        }

        valueFound = cleanSearchable(valueFound);
        valueLookedFor = cleanSearchable(valueLookedFor);

        // Compare with the operator
        boolean pass = false;
        switch(Operator.fromChar(operator)) {
            case CONTAINS:
                pass = valueFound.contains(valueLookedFor);
                break;
            case EQUAL:
                pass = valueFound.equals(valueLookedFor);
                break;
            default:
                pass = false;
        }

        return pass;
    }

    public List<Media> getMatches() {
        List<Media> media = new ArrayList<>();
        for (Media m : allMedia)
            if (isMatchForMedia(query,m))
                media.add(m);
        return media;
    }

    public String getDescription() {
        return description;
    }
}
