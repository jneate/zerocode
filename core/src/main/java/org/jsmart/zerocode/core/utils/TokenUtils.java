package org.jsmart.zerocode.core.utils;

import org.apache.commons.lang.text.StrSubstitutor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static org.jsmart.zerocode.core.engine.preprocessor.ZeroCodeTokens.*;

public class TokenUtils {
	
	private static HashMap<Integer, ArrayList<String>> generatedNumbers = new HashMap<>();

    public static String resolveKnownTokens(String requestJsonOrAnyString) {
        Map<String, Object> paramMap = new HashMap<>();

        final List<String> testCaseTokens = getTestCaseTokens(requestJsonOrAnyString);

        testCaseTokens.forEach(runTimeToken -> {
            populateParamMap(paramMap, runTimeToken);
        });

        StrSubstitutor sub = new StrSubstitutor(paramMap);

        return sub.replace(requestJsonOrAnyString);
    }

    public static void populateParamMap(Map<String, Object> paramaMap, String runTimeToken) {
        getKnownTokens().forEach(inStoreToken -> {
                    if (runTimeToken.startsWith(inStoreToken)) {
                        if (runTimeToken.startsWith(RANDOM_NUMBER)) {
                        	// Option to include length for RANDOM_NUMBER generation
                        	if (runTimeToken.contains(":")) {
                        		int length = Integer.parseInt(runTimeToken.substring(RANDOM_NUMBER.length() + 1));
                        		paramaMap.put(runTimeToken, createRandomNumber(length));
                        	} else {
                        		paramaMap.put(runTimeToken, System.currentTimeMillis() + "");
                        	}
                        } else if (runTimeToken.startsWith(RANDOM_STRING_PREFIX)) {
                            int length = Integer.parseInt(runTimeToken.substring(RANDOM_STRING_PREFIX.length()));
                            paramaMap.put(runTimeToken, createRandomAlphaString(length));

                        } else if (runTimeToken.startsWith(STATIC_ALPHABET)) {
                            int length = Integer.parseInt(runTimeToken.substring(STATIC_ALPHABET.length()));
                            paramaMap.put(runTimeToken, createStaticAlphaString(length));

                        } else if (runTimeToken.startsWith(LOCALDATE_TODAY)) {
                            String formatPattern = runTimeToken.substring(LOCALDATE_TODAY.length());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
                            paramaMap.put(runTimeToken, LocalDate.now().format(formatter));

                        } else if (runTimeToken.startsWith(LOCALDATETIME_NOW)) {
                            String formatPattern = runTimeToken.substring(LOCALDATETIME_NOW.length());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
                            paramaMap.put(runTimeToken, LocalDateTime.now().format(formatter));

                        } else if (runTimeToken.startsWith(XML_FILE)) {
                            String xmlFileResource = runTimeToken.substring(XML_FILE.length());
                            final String xmlString = getXmlContent(xmlFileResource);
                            // Used escapeJava, do not use escapeXml as it replaces
                            // with GT LT etc ie what exactly you don't want
                            paramaMap.put(runTimeToken, escapeJava(xmlString));

                        } else if (runTimeToken.startsWith(RANDOM_UU_ID)) {
                            paramaMap.put(runTimeToken, randomUUID().toString());
                        }
                    }
                }
        );

    }

    /**
     * This method was introduced later,
     * But Framework uses- ZeroCodeJsonTestProcesorImpl#getTestCaseTokens(java.lang.String)
     */
    public static List<String> getTestCaseTokens(String aString) {

        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(aString);

        List<String> keyTokens = new ArrayList<>();

        while (matcher.find()) {
            keyTokens.add(matcher.group(1));
        }

        return keyTokens;
    }

    public static String createRandomAlphaString(int length) {
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < length; i++) {
            builder.append((char) ('a' + r.nextInt(26)));
        }
        String randomString = builder.toString();
        return randomString;
    }

    public static String createStaticAlphaString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append((char) ('a' + i));

            /*
             * This will repeat after A to Z
             */
            i = i >= 26 ? 0 : i;
        }

        return builder.toString();
    }
    
    public static String createRandomNumber(int length) {
    	
    	//Generate the maximum Long value based on the length
    	long maxSize = (long) Math.pow(10, length) - 1;
    	
    	//Check to see if the HashMap is empty or this particular length hasn't been called in this test suite
    	if (generatedNumbers.isEmpty() || generatedNumbers.get(length) == null) {
    		
    		generatedNumbers.put(length, new ArrayList<String>());
    		
    		StringBuilder first = new StringBuilder("1");
    		
    		// Generate the smallest number based on the input length
    		for (int i = 1; i < length; i++) {
    			first.append("0");
    		}
    		
    		// Add the smallest number as the first item in the Array for that length
    		generatedNumbers.get(length).add(first.toString());
    		
    	} else {
    		
    		// Retrieve the previous value
    		long previousValue = Long.parseLong(generatedNumbers.get(length).get(generatedNumbers.get(length).size() - 1));
    		
    		// Check if the Previous Value + 1 exceeds the maximum size
    		if (previousValue + 1 > maxSize) {
    			// Do something on ERROR
    		} else {
    			// Add the new value to the Array
    			generatedNumbers.get(length).add(Long.toString(previousValue + 1));
    		}
    		
    	}
    	
    	// Return the most recent generated value
    	return generatedNumbers.get(length).get(generatedNumbers.get(length).size() - 1);
    	
    }

    public static String getXmlContent(String xmlFileResource) {
        try {
            return SmartUtils.readJsonAsString(xmlFileResource);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Oops! Problem occurred while reading the XML file '" + xmlFileResource
                    + "', details:" + e);
        }
    }
}
