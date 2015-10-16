package com.wdimiceli;

import com.sun.javafx.binding.StringFormatter;
import org.codehaus.groovy.syntax.ReadException;

import java.io.File;
import java.nio.file.*;
import java.util.*;

/**
 * Created by Wes on 7/22/2014.
 */
public class CmdLineArgs {
    public static class Argument {
        private String name;
        private String description;
        private ArgumentHandler handler;
        private Object value;
        private String defaultValue;

        public String toUsageString(String prepend) {
            StringBuilder builder = new StringBuilder();

            builder.append(prepend);
            builder.append(String.format("%s: %s", name, description));
            builder.append("\n");

            if (defaultValue != null) {
                builder.append(prepend);
                builder.append(String.format("\tDefault: %s", defaultValue.toString()));
                builder.append("\n");
            }

            String handlerUsage = handler.toUsageString(prepend);
            if (handlerUsage != null && handlerUsage.length() > 0) {
                builder.append("\t");
                builder.append(handlerUsage);
                builder.append("\n");
            }

            return builder.toString();
        }

        public Object getValue() {
            return value;
        }

        public Argument setValue(Object v) {
            value = v;
            return this;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public ArgumentHandler getHandler() {
            return handler;
        }

        public String getName() {
            return name;
        }

        public Argument(String in_name, String in_description, ArgumentHandler in_handler) {
            this(in_name, in_description, in_handler, null);
        }

        public Argument(String in_name, String in_description, ArgumentHandler in_handler, String in_defaultValue) {
            name = in_name;
            description = in_description;
            handler = in_handler;
            defaultValue = in_defaultValue;
            value = null;
        }
    }
    public static interface ArgumentHandler {
        public Object tryArgument(String arg) throws Exception;
        public String toUsageString(String prepend);
    }
    /*
    Parse integers and contrain to a range of values

    Outputs null if the argument cannot be parsed
     */
    public static class IntegerRangeArgumentHandler implements ArgumentHandler {
        public final int minimum;
        public final int maximum;

        public Object tryArgument(String arg) throws Exception{
            try {
                int value = Integer.parseInt(arg);
                if (value < minimum || value > maximum) {
                    throw new Exception(String.format("Out of range [%d, %d]", minimum, maximum));
                }
                return value;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public String toUsageString(String prepend) {
            return prepend + String.format("Range from [%d, %d]", minimum, maximum);
        }

//        public static int getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof Integer;
//            return (Integer) args.getParameter(s);
//        }

        public IntegerRangeArgumentHandler(int in_minimum, int in_maximum) {
            minimum = in_minimum;
            maximum = in_maximum;
        }
    }
    public static class IntegerChoiceArgumentHandler extends IntegerRangeArgumentHandler {
        private HashMap<String, Integer> choiceMapping;

        @Override
        public Object tryArgument(String arg) throws Exception {
            Object value = super.tryArgument(arg);
            if (value == null && choiceMapping.containsKey(arg)) {
                value = choiceMapping.get(arg);
            }
            return value;
        }

        @Override
        public String toUsageString(String prepend) {
            return super.toUsageString(prepend) + String.format(" or %s", choiceMapping.keySet().toString());
        }

        public IntegerChoiceArgumentHandler(int in_minimum, int in_maximum, HashMap<String, Integer> choices) {
            super(in_minimum, in_maximum);
            choiceMapping = choices;
        }
    }
    /*
    Accepts any string at face value
     */
    public static class identityArgumentHandler implements ArgumentHandler {
        public Object tryArgument(String arg) throws Exception {
            return arg;
        }

        public String toUsageString(String prepend) {
            return null;
        }

//        public static String getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof String;
//            return (String) args.getParameter(s);
//        }
    }
    /*
    Allows choice of a given set of strings

    Outputs null if the argument was not among the choices specified
     */
    public static class ChoiceArgumentHandler implements ArgumentHandler {
        private Set<String> choices;

        public Object tryArgument(String arg) throws Exception {
            if (choices.contains(arg)) {
                return arg;
            } else {
                return null;
            }
        }

        public String toUsageString(String prepend) {
            return prepend + String.format("Accepted values: %s", choices.toString());
        }

//        public static String getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof String;
//            return (String) args.getParameter(s);
//        }

        public ChoiceArgumentHandler(String[] in_choices) {
            choices = new HashSet<String>(Arrays.asList(in_choices));
        }
        public ChoiceArgumentHandler(Set<String> in_choices) {
            choices = in_choices;
        }
    }
    /*
    Parses the path and output a File object

    Will throw an exception if failed.  Cannot be chained.
     */
    public static class PathArgumentHandler implements ArgumentHandler {
        private boolean mustExist;

        public Object tryArgument(String arg) throws Exception {
            try {
                Path p = FileSystems.getDefault().getPath(arg);
                File f = p.toFile();
                if (mustExist && !f.exists()) {
                    throw new InvalidPathException(arg, "file must exist");
                }
                return f;
            } catch (InvalidPathException e) {
                throw e;
            }
        }

        public String toUsageString(String prepend) {
            return null;
        }

//        public static File getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof File;
//            return (File) args.getParameter(s);
//        }
        public PathArgumentHandler(boolean in_mustExist) {
            mustExist = in_mustExist;
        }
    }
    /*
    Accepts boolean values

    Can be t, true, yes, y, or 1 for true
    Or f, false, no, n, or 0 for false

    Outputs null if none of the above values are presented
     */
    public static class BooleanArgumentHandler implements ArgumentHandler {
        private static final String[] trueValues = {"t", "true", "yes", "y", "1"};
        private static final String[] falseValues = {"f", "false", "no", "n", "0"};

        private boolean testStrings(String[] cases, String target) {
            for (String s : cases) {
                if (target.equalsIgnoreCase(s)) return true;
            }
            return false;
        }

        public Object tryArgument(String arg) throws Exception {
            if (testStrings(trueValues, arg)) {
                return true;
            } else if (testStrings(falseValues, arg)) {
                return false;
            } else {
                return null;
            }
        }

        public String toUsageString(String prepend) {

            return prepend + String.format("Accepts: %s or %s", Arrays.asList(trueValues).toString(), Arrays.asList(falseValues).toString());
        }

//        public static Boolean getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof Boolean;
//            return (Boolean) args.getParameter(s);
//        }
    }

    /*
    Parses a list of items delimited by a given string by another handler

    Outputs null if only one argument, and the given handler also outputs null
     */
    public static class DelimitedArgumentHandler implements ArgumentHandler {
        private ArgumentHandler handler;
        private int maxEntries;
        private String separator;

        public Object tryArgument(String arg) throws Exception {
            try {
                ArrayList<Object> retval = new ArrayList<Object>();
                String[] values = arg.split(separator);
                if (values.length == 0) {
                    throw new Exception("Invalid argument");
                }
                if (values.length == 1) {
                    return handler.tryArgument(values[0]);
                }
                if (values.length > maxEntries) {
                    throw new Exception(String.format("Cannot exceed %d entries", maxEntries));
                }
                for (String v : values) {
                    Object param = handler.tryArgument(v);
                    if (param == null) {
                        throw new Exception(String.format("Invalid member of list: %s", v));
                    }
                    retval.add(param);
                }
                return retval;
            } catch (Exception e) {
                throw e;
            }
        }

        public String toUsageString(String prepend) {
            return null;
        }

//        public static ArrayList<Object> getParameter(String s, CmdLineArgs args) {
//            Object param = args.getParameter(s);
//            assert param instanceof ArrayList;
//            return (ArrayList<Object>) args.getParameter(s);
//        }

        public DelimitedArgumentHandler(String in_separator, int in_maxEntries, ArgumentHandler in_handler) {
            assert in_handler != null && in_separator != null;
            separator = in_separator;
            handler = in_handler;
            maxEntries = in_maxEntries;
        }
    }

    //final parameters
    private HashMap<String, Argument> parameters;

    /*
    verifies that each of our registered parameters has a value, either given or default

    throws an exeption otherwise
     */
    private void verify() throws Exception {
        for (Argument parameter : parameters.values()) {
            if (parameter.getValue() == null && parameter.getDefaultValue() == null) {
                throw new Exception(String.format("Argument '%s' is required.  Please see usage.", parameter.name));
            }
        }
    }

    /*
    Tries each handler for the given parameter and returns a success boolean
     */
    private boolean parseKV(String key, String value) throws Exception {
        boolean success = false;
        //must have a registered handler; see registerArgumentHandler()
        if (!parameters.containsKey(key)) {
            throw new Exception(key + " is not a valid argument");
        }
        Argument arg = parameters.get(key);
        //don't allow dupes
        if (arg.getValue() != null) {
            throw new Exception("Duplicate arguments: " + key);
        }
        try {
            //handler returns null if it can't handle the argument
            //it can throw an exception if the argument is an unacceptable value
            Object param = arg.getHandler().tryArgument(value);
            if (param != null) {
                arg.setValue(param);
                success = true;
            }
        } catch (Exception e) {
            throw new Exception("Bad argument value for '"+key+"': " + e.getMessage());
        }
        return success;
    }

    /*
    Takes a list of command line arguments and runs them through the handlers

    Will throw an exception if the key/value formatting is incorrect
        or if the list has duplicate arguments ( thie one forwarded from parseKV() )
        or if the there is no handler registered ( thie one also forwarded from parseKV() )
     */
    public void parseArgs(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            //split each arugument into key=value parameters
            String arg = args[i];
            String[] parts = arg.split("=");
            if (parts.length != 2) {
                throw new Exception("Argument has too many parts: " + arg);
            }

            String key = parts[0].toLowerCase();
            String value = parts[1];
            if (key.length() == 0 || value.length() == 0) {
                throw new Exception("Failed to parse argument: " + arg);
            }

            //parseKV() returns a boolean success value
            if (!parseKV(key, value)) {
                //we reach this point if all of the handlers for this paramters have returned null
                throw new Exception(String.format("Argument '%s' is invalid. Please see usage.", arg));
            }
        }
        verify();
    }

    public Object getValue(String argumentName) {
        Object value = null;
        argumentName = argumentName.toLowerCase();
        if (parameters.containsKey(argumentName)) {
            Argument argument = parameters.get(argumentName);
            value = argument.getValue();
            if (value == null) {
                String defaultValue = argument.getDefaultValue();
                try {
                    value = argument.getHandler().tryArgument(defaultValue);
                } catch (Exception e) {}
            }
        }
        return value;
    }

    public String toUsageString() {
        StringBuilder sb = new StringBuilder();
        for (Argument a : parameters.values()) {
            sb.append(a.toUsageString("\t"));
        }
        return sb.toString();
    }

    /*
    Register a new Argument handler for a desired parameter
     */
    public void registerArgument(Argument argument) {
        parameters.put(argument.getName().toLowerCase(), argument);
    }

    public CmdLineArgs() {
        parameters = new HashMap<String, Argument>();
    }
}
