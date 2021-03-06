/**
 Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

 http://aws.amazon.com/apache2.0/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package main.java.com.ATF.skill;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import main.java.com.ATF.storage.AddictionTreatmentFinder;
import main.java.com.ATF.storage.AddictionTreatmentFinderDao;
import main.java.com.ATF.storage.AddictionTreatmentFinderDynamoDbClient;
import main.java.com.ATF.storage.AddictionUserData;
import main.java.com.ATF.storage.main.java.com.ATF.storage.addictioncenters.AddictionCentersData;
import main.java.com.ATF.storage.main.java.com.ATF.storage.addictioncenters.AddictionCentersFinderDao;
import main.java.com.ATF.storage.main.java.com.ATF.storage.addictioncenters.AddictionCentersFinderDynamoDbClient;
import main.java.com.ATF.utils.DBLoader;
import main.java.com.ATF.utils.PropertyReader;
import main.java.com.ATF.utils.twilio.TwilioCallText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The {@link AddictionTreatmentFinderManager} receives various events and intents and manages the flow of the
 * game.
 */
public class AddictionTreatmentFinderManager {
    private static PropertyReader propertyReader = PropertyReader.getPropertyReader();
    private static final Logger log = LoggerFactory.getLogger(AddictionTreatmentFinderManager.class);

    /**
     * Intent slot for player name.
     */
    private static final String SLOT_USER_NAME = "UserName";

    private static final String SLOT_USER_AGE = "UserAge";
    private static final String SLOT_USER_CITY = "City";
    private static final String SLOT_USER_STATE = "State";
    private static final String SLOT_USER_MEDICAL_RELIGIOUS_CHOICE = "ReligiousMedicalChoice";
    private static final String SLOT_PHONE_NUMBER = "PhoneNo";

    /**
     * Intent slot for player score.
     */
    private static final String SLOT_SCORE_NUMBER = "ScoreNumber";


    /**
     * Maximum number of players for which scores must be announced while adding a score.
     */
    private static final int MAX_PLAYERS_FOR_SPEECH = 3;

    private final AddictionTreatmentFinderDao addictionTreatmentFinderDao;
    private final AddictionCentersFinderDao addictionCentersFinderDao;


    public AddictionTreatmentFinderManager (final AmazonDynamoDBClient amazonDynamoDbClient) {
        AddictionTreatmentFinderDynamoDbClient dynamoDbClient =
                new AddictionTreatmentFinderDynamoDbClient(amazonDynamoDbClient);
        AddictionCentersFinderDynamoDbClient addictionCentersDynamoDbClient =
                new AddictionCentersFinderDynamoDbClient(amazonDynamoDbClient);


        addictionTreatmentFinderDao = new AddictionTreatmentFinderDao(dynamoDbClient);
        addictionCentersFinderDao = new AddictionCentersFinderDao(addictionCentersDynamoDbClient);
    }



    public SpeechletResponse getConnectIntentResponse (Intent intent, Session session, SkillContext skillContext) {
        log.debug("In getConnectIntentResponse");

        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = null;

        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }

        int iStepInSession = stepInSession.intValue();
        log.debug("Entered getConnectIntentResponse With STEP in Session value of - " + stepInSession.intValue());

        addictionUserData = skillContext.getAddictionUserData();

        log.debug("in getConnectIntentResponse 1 " + skillContext.getAddictionUserData().toString());

        AddictionTreatmentFinder treatmentFinder = addictionTreatmentFinderDao.getUserData(session);
        if (treatmentFinder == null) {
            treatmentFinder = AddictionTreatmentFinder.newInstance(session, addictionUserData);
        }
        treatmentFinder.setUserData(addictionUserData);

        String prompt = "Thanks for using the skill. You will recieve a call shortly.";

        String ifNationalFacilityCase = (String) session.getAttribute("SESSION-NATIONAL-FACILITY");
        log.debug("in getConnectIntentResponse 2 " + ifNationalFacilityCase);

        if (ifNationalFacilityCase != null && ifNationalFacilityCase.equalsIgnoreCase("yes")) {
            TwilioCallText twilioCallText = new TwilioCallText();
            twilioCallText.connectCall("+19149537025");

           //todo Connection the call with National Facility Number
            log.debug("TODo - Connect the call with National Facility Number");
        } else {

            //todo search of the center with state, city data
            //todo place the call

            /*List<AddictionCentersData> addictionCenterDataList = addictionCentersFinderDao.scanCityState(session, propertyReader.getAlcoholCentersTable());

            for (AddictionCentersData addictionCenterData : addictionCenterDataList) {
                log.debug("TODo - Search the center with state and city, connect the call");
                log.debug(addictionCenterData.toString());
            }*/


        }

        // Save the updated game
        addictionTreatmentFinderDao.setUserData(treatmentFinder, session);

//        return getAskSpeechletResponse(propertyReader.getSpeechConnect(), propertyReader.getSpeechReprompt());
        return getTellSpeechletResponse(treatmentFinder.getUserData().getfName() + " " + prompt);

    }

    public SpeechletResponse getSearchIntentResponse (Intent intent, Session session, SkillContext skillContext) {
        log.debug("In getConnectIntentResponse");

        // Load the previous game
        AddictionTreatmentFinder treatmentFinder = addictionTreatmentFinderDao.getUserData(session);

        if (treatmentFinder == null) {
            treatmentFinder = AddictionTreatmentFinder.newInstance(session, skillContext.getAddictionUserData());
        }

        log.debug("in getSearchIntentResponse 1 " + skillContext.getAddictionUserData().toString());
        treatmentFinder.setUserData(skillContext.getAddictionUserData());

        log.debug("in getSearchIntentResponse 2 " + skillContext.getAddictionUserData().toString());
        log.debug("in getSearchIntentResponse 3 " + treatmentFinder);
        log.debug("in getSearchIntentResponse 4 " + treatmentFinder.getUserData());

        String prompt = "Thanks for using the skill. You will recieve a text with details shortly.";

        String ifNationalFacilityCase = (String) session.getAttribute("SESSION-NATIONAL-FACILITY");
        if (ifNationalFacilityCase != null && ifNationalFacilityCase.equalsIgnoreCase("yes")) {
            TwilioCallText twilioCallText = new TwilioCallText();
            twilioCallText.sendText(propertyReader.getTwilioNumber(), skillContext.getAddictionUserData().getPhoneNumber(), propertyReader.getNationalFacilityTextMessageContent());

            log.debug("Just Texted the details with National Facility Number");
        } else {
            AddictionCentersData addictionCenterData = null;
            ScanResult scanResult = addictionCentersFinderDao.scanCityState(session, propertyReader.getAlcoholCentersTable());
            List<Map<String, AttributeValue>> items;
            Map<String, AttributeValue> itemMap;
            Collection<AttributeValue> attributeValues;
            Set keysArray;
            String currentAttributeValue;

            AddictionCentersData addictionCentersData;
            List addictionCentersList = new ArrayList();

            if (scanResult != null && scanResult.getItems() != null) {
                items = scanResult.getItems();
                log.debug("ITEMs are - " + items);

                for (int i=0; i<items.size(); i++) {
                    itemMap = items.get(i);
                    log.debug("Current one is - " + itemMap);
                    log.debug("Data of the Current one is - " + itemMap.get("Data"));
                    log.debug("String value of yhe Data of the Current one is - " + itemMap.get("Data").getS());

                    addictionCenterData = unmarshall(itemMap.get("Data").getS());
                    addictionCentersList.add(addictionCenterData);
                    log.debug("addictionCenterData value of yhe Data of the Current one is - " + addictionCenterData.toString());
                }
                if (items.size()>0) {
                    addictionCenterData = findCorrectCenter(addictionCentersList, session);
                }
            }

            if (addictionCenterData != null) {
                TwilioCallText twilioCallText = new TwilioCallText();
                twilioCallText.sendText(propertyReader.getTwilioNumber(), skillContext.getAddictionUserData().getPhoneNumber(), propertyReader.getTextMessageContent() + " " + addictionCenterData.getPhoneNumber() + " " + propertyReader.getGoodBye());
            } else {
                prompt = "We were unable to find the appropriate center. Please try again.";
            }

        }

        // Save th data
        addictionTreatmentFinderDao.setUserData(treatmentFinder, session);


//        return getAskSpeechletResponse(propertyReader.getSpeechConnect(), propertyReader.getSpeechReprompt());
        return getTellSpeechletResponse(treatmentFinder.getUserData().getfName() + ". " + prompt);
    }


    /**
     * Creates and returns response for Launch request.
     *
     * @param request
     *            {@link LaunchRequest} for this request
     * @param session
     *            Speechlet {@link Session} for this request
     * @return response for launch request
     */
    public SpeechletResponse getLaunchResponse(LaunchRequest request, Session session) {
        // Speak welcome message and ask user questions
        // based on whether there are players or not.

/* Commenting till i know better TODO
        String speechText, repromptText;
        AddictionCentersFinder game = addictionTreatmentFinderDao.getUserData(session);

        if (game == null || !game.hasPlayers()) {
            speechText = "ScoreKeeper, Let's start your game. Who's your first player?";
            repromptText = "Please tell me who is your first player?";
        } else if (!game.hasScores()) {
            speechText =
                    "ScoreKeeper, you have " + game.getNumberOfPlayers()
                            + (game.getNumberOfPlayers() == 1 ? " player" : " players")
                            + " in the game. You can give a player points, add another player,"
                            + " reset all players or exit. Which would you like?";
            repromptText = AddictionTreatmentFinderTextUtil.COMPLETE_HELP;
        } else {
            speechText = "ScoreKeeper, What can I do for you?";
            repromptText = AddictionTreatmentFinderTextUtil.NEXT_HELP;
        }

        return getAskSpeechletResponse(speechText, repromptText);
*/

//Todo remove after loading date
        AddictionCentersData addictionCentersData = AddictionCentersData.newInstance();

//Populate the data List
        DBLoader dbLoader = new DBLoader();
        ArrayList list = dbLoader.getDBLoaderList();

        //Populate the data List
        String value = null;

        for (int i=0; i<list.size(); i++) {
            value = (String) list.get(i);
            addictionCentersData = unmarshall(value);
            log.debug("Fefore saving        " + addictionCentersData.toString());
            addictionCentersFinderDao.loadCentersTable(addictionCentersData);

        }
//Todo remove after loading date

        return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
    }

    public SpeechletResponse getStopIntentResponse (Intent intent, Session session, SkillContext skillContext) {
        String isFalseNo = (String) session.getAttribute("SESSION-NO-DONT-EXIT");

        if (isFalseNo != null && isFalseNo.equalsIgnoreCase("yes")) {
            //session.removeAttribute("SESSION-NO-DONT-EXIT");
            log.debug("Landed in the STOP intent in case of a false no");

            Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
            AddictionUserData addictionUserData = null;

            if (stepInSession == null) {
                return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
            }

            int iStepInSession = stepInSession.intValue();
            log.debug("Entered getStopIntentResponse With STEP in Session value of - " + stepInSession.intValue());

            if (iStepInSession == 4) {
                addictionUserData = skillContext.getAddictionUserData();

                addictionUserData.setLgbt("no");
                addictionUserData.setQuestionPhase(iStepInSession + 1); //set the state of the next question
                session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
                session.setAttribute("SESSION-USER-LGBT", addictionUserData.getLgbt());
                log.debug("In 4 Addiction data - " + addictionUserData.toString());

                skillContext.setAddictionUserData(addictionUserData);
                return getAskSpeechletResponse(addictionUserData.getfName() +", " + propertyReader.getQuestion4(), propertyReader.getQuestion4());
            } else if (iStepInSession == 5) {
                addictionUserData = skillContext.getAddictionUserData();

                addictionUserData.setPregnantOrSpecalizedCare("no");
                addictionUserData.setQuestionPhase(iStepInSession + 1); //set the state of the next question
                session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
                session.setAttribute("SESSION-USER-MEDICAL-ASSISTANCE", addictionUserData.getPregnantOrSpecalizedCare());
                log.debug("In 5 Addiction data - " + addictionUserData.toString());
                session.removeAttribute("SESSION-NO-DONT-EXIT");

                skillContext.setAddictionUserData(addictionUserData);
                return getAskSpeechletResponse(addictionUserData.getfName() +", " + propertyReader.getQuestion5(), propertyReader.getQuestion5());
            }

        }
        return getExitIntentResponse(intent, session, skillContext);
    }



    /**
     * Creates and returns response for the new game intent.
     *
     * @param session
     *            {@link Session} for the request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the new game intent.
     */
    public SpeechletResponse getStartInteractionIntentResponse (Intent intent, Session session, SkillContext skillContext) {
        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = null;

        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }

        int iStepInSession = stepInSession.intValue();
        log.debug("Entered getStartInteractionIntentResponse With STEP in Session value of - " + stepInSession.intValue());

        if (iStepInSession == 0) {
            addictionUserData = skillContext.getAddictionUserData();
            if (addictionUserData == null) {
                addictionUserData = AddictionUserData.newInstance();
            }
            addictionUserData.setQuestionPhase(iStepInSession+1); //set the state ofspeech-welcome the next question

            log.debug("In 0 Addiction data - " + addictionUserData.toString());

            session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
            skillContext.setAddictionUserData(addictionUserData);

            return getAskSpeechletResponse(propertyReader.getQuestion0(), propertyReader.getQuestion0());

        } else if (iStepInSession == 1) {
            addictionUserData = skillContext.getAddictionUserData();
            addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question
            session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));

            log.debug("In 1 Addiction data - " + addictionUserData.toString());

            skillContext.setAddictionUserData(addictionUserData);
            return getAskSpeechletResponse(propertyReader.getQuestion1(), propertyReader.getQuestion1());
        } else if (iStepInSession == 4) {
            addictionUserData = skillContext.getAddictionUserData();

            addictionUserData.setLgbt("yes");
            addictionUserData.setQuestionPhase(iStepInSession + 1); //set the state of the next question
            session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
            session.setAttribute("SESSION-USER-LGBT", addictionUserData.getLgbt());
            log.debug("In 4 Addiction data - " + addictionUserData.toString());
            session.setAttribute("SESSION-NATIONAL-FACILITY", "yes");
            session.setAttribute("SESSION-NO-DONT-EXIT", "yes");

            skillContext.setAddictionUserData(addictionUserData);
            return getAskSpeechletResponse(addictionUserData.getfName() +", " + propertyReader.getQuestion4(), propertyReader.getQuestion4());
        } else if (iStepInSession == 5) {
            addictionUserData = skillContext.getAddictionUserData();

            addictionUserData.setPregnantOrSpecalizedCare("yes");
            addictionUserData.setQuestionPhase(iStepInSession + 1); //set the state of the next question
            session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
            session.setAttribute("SESSION-USER-MEDICAL-ASSISTANCE", addictionUserData.getPregnantOrSpecalizedCare());
            log.debug("In 5 Addiction data - " + addictionUserData.toString());
            session.setAttribute("SESSION-NATIONAL-FACILITY", "yes");
            session.setAttribute("SESSION-NO-DONT-EXIT", "yes");

            skillContext.setAddictionUserData(addictionUserData);
            return getAskSpeechletResponse(addictionUserData.getfName() +", " + propertyReader.getQuestion5(), propertyReader.getQuestion5());
        } else if (iStepInSession == 7) {
            /*addictionUserData = skillContext.getAddictionUserData();

            String phoneNumber = "";
            Map slots = intent.getSlots();
            Collection keys = slots.values();
            Iterator keysIterator = keys.iterator();
            while (keysIterator.hasNext()) {
                String slotName = (String) slots.get(keysIterator.next());
                log.debug("getCapturePhoneIntentResponse iterating over keys " + slotName);
            }
            String phoneNumberOne = intent.getSlot(SLOT_PHONE_NUMBER).getValue();

            addictionUserData.setQuestionPhase(iStepInSession + 1); //set the state of the next question
            session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));

            skillContext.setAddictionUserData(addictionUserData);*/
            return getAskSpeechletResponse(addictionUserData.getfName() +", " + "I am here", "I am here");
        } else {
            return getTellSpeechletResponse(propertyReader.getFatalError());
        }
    }

    public SpeechletResponse getCaptureCityStateIntentResponse (Intent intent, Session session,
                                                                      SkillContext skillContext) {
        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = skillContext.getAddictionUserData();

        log.debug("getCaptureCityStateIntentResponse STEP in Session - " + stepInSession.intValue());
        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }
        int iStepInSession = stepInSession.intValue();
        String city = intent.getSlot(SLOT_USER_CITY).getValue();
        String state = intent.getSlot(SLOT_USER_STATE).getValue();

        if (city == null || state == null) {
            String speechText = "Sorry, I did not hear that" + propertyReader.getQuestion7();
            return getAskSpeechletResponse(speechText, speechText);
        }

        addictionUserData.setCity(city);
        addictionUserData.setState(state);

        log.debug("In getCaptureCityStateIntentResponse Logging Addiction contents - : " + addictionUserData.toString());

        addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question

        skillContext.setAddictionUserData(addictionUserData);
        session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
        session.setAttribute("SESSION-CITY", addictionUserData.getCity());
        session.setAttribute("SESSION-STATE", addictionUserData.getState());

        return getAskSpeechletResponse(addictionUserData.getfName()+", " + propertyReader.getQuestion9(), propertyReader.getQuestion9());
    }

    public SpeechletResponse getReligiousMedicalChoiceIntentResponse (Intent intent, Session session,
                                                       SkillContext skillContext) {
        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = skillContext.getAddictionUserData();

        log.debug("getReligiousMedicalChoiceIntentResponse STEP in Session - " + stepInSession.intValue());
        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }
        int iStepInSession = stepInSession.intValue();

        String religiousMedicalChoice = intent.getSlot(SLOT_USER_MEDICAL_RELIGIOUS_CHOICE).getValue();

        if (religiousMedicalChoice == null) {
            String speechText = "Sorry, I did not hear that" + propertyReader.getQuestion5();
            return getAskSpeechletResponse(speechText, speechText);
        }

        addictionUserData.setReligiousMedical(religiousMedicalChoice);

        log.debug("In getReligiousMedicalChoiceIntentResponse Logging Addiction contents - : " + addictionUserData.toString());

        addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question

        skillContext.setAddictionUserData(addictionUserData);
        session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
        session.setAttribute("SESSION-USER-MEDICAL-CHOICE", addictionUserData.getReligiousMedical());

        return getAskSpeechletResponse(addictionUserData.getfName()+", " + propertyReader.getQuestion8(), propertyReader.getQuestion8());

    }

    public SpeechletResponse getCapturePhoneIntentResponse (Intent intent, Session session,
                                                       SkillContext skillContext) {
        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = skillContext.getAddictionUserData();

        log.debug("getCapturePhoneIntentResponse STEP in Session - " + stepInSession.intValue());

        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }
        int iStepInSession = stepInSession.intValue();


        Map slots = intent.getSlots();
        Collection keys = slots.values();
        Iterator keysIterator = keys.iterator();
        while (keysIterator.hasNext()) {
            String slotName = (String) slots.get(keysIterator.next());
            log.debug("getCapturePhoneIntentResponse iterating over keys " + slotName);
        }
        String phoneNumber = intent.getSlot(SLOT_PHONE_NUMBER).getValue();

        if (phoneNumber == null) {
            String speechText = "Sorry, I did not hear that" + propertyReader.getQuestion8();
            return getAskSpeechletResponse(speechText, speechText);
        }

        String userName = addictionUserData.getfName();
        log.debug("in getCapturePhoneIntentResponse phoneNumber inputted is - " + phoneNumber);
        if (phoneNumber == null) {
            return getAskSpeechletResponse(userName+", " + propertyReader.getQuestion8(), propertyReader.getQuestion8());
        }
        addictionUserData.setPhoneNumber(phoneNumber);

        log.debug("In getCapturePhoneIntentResponse Logging Addiction contents - : " + addictionUserData.toString());

        addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question

        log.debug("In getCapturePhoneIntentResponse addiction data - " + addictionUserData.getAgeType());
        skillContext.setAddictionUserData(addictionUserData);
        session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
        session.setAttribute("SESSION-USER-AGE", addictionUserData.getAgeType());
        session.setAttribute("SESSION-USER-PHONE", addictionUserData.getPhoneNumber());

        String sessionNationalFacility = (String) session.getAttribute("SESSION-NATIONAL-FACILITY");
        String prompt = null;
        if (sessionNationalFacility != null && sessionNationalFacility.equalsIgnoreCase("yes")) {
            prompt = propertyReader.getQuestion6();
        } else {
            prompt = propertyReader.getQuestion7();
        }
//        kjgfhgckjhhfjh
        // Get number (08)

        // If the session has "SESSION-NATIONAL-FACILITY" load Q6 and in connect/search send them SESSION-NATIONAL-FACILITY number

        // if the session does not have SESSION-NATIONAL-FACILITY, load Q7, Q9

        // Q6 and Q9 will land the user at ConnectCallIntent and SearchCallIntent
        return getAskSpeechletResponse(userName+", " + prompt, prompt);
    }


    public SpeechletResponse getUserAgeIntentResponse (Intent intent, Session session,
                                                       SkillContext skillContext) {

        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = skillContext.getAddictionUserData();

        log.debug("getUserAgeIntentResponse STEP in Session - " + stepInSession.intValue());

        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }
        int iStepInSession = stepInSession.intValue();

        // add a player to the current game,
        // terminate or continue the conversation based on whether the intent
        // is from a one shot command or not.
        String userAge = intent.getSlot(SLOT_USER_AGE).getValue();

        if (userAge == null) {
            String speechText = "Sorry, I did not hear that" + propertyReader.getQuestion1();
            return getAskSpeechletResponse(speechText, speechText);
        }

        String newUserName = addictionUserData.getfName();
        log.debug("in getUserAgeIntentResponse userAge inputted is - " + userAge);
        if (userAge == null) {
            return getAskSpeechletResponse(newUserName+", " + propertyReader.getQuestion1(), propertyReader.getQuestion1());
        }
        addictionUserData.setAgeType(userAge);

        log.debug("In getUserAgeIntentResponse Logging Addiction contents - : " + addictionUserData.toString());

        addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question

        log.debug("In getUserAgeIntentResponse addiction data - " + addictionUserData.getAgeType());
        skillContext.setAddictionUserData(addictionUserData);
        session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
        session.setAttribute("SESSION-USER-AGE", addictionUserData.getAgeType());
        if (userAge.equalsIgnoreCase("Under 18")) {
            session.setAttribute("SESSION-NATIONAL-FACILITY", "yes");
        }
        session.setAttribute("SESSION-NO-DONT-EXIT", "yes");
        return getAskSpeechletResponse(newUserName+", " + propertyReader.getQuestion3(), propertyReader.getQuestion3());
    }


    /**
     * Creates and returns response for the add player intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            Speechlet {@link Session} for this request
     * @param skillContext
     * @return response for the add player intent.
     */
    public SpeechletResponse getAddUserIntentResponse (Intent intent, Session session,
                                                       SkillContext skillContext) {

        Integer stepInSession = (Integer) session.getAttribute("SESSION-STEP");
        AddictionUserData addictionUserData = skillContext.getAddictionUserData();

        log.debug("getAddUserIntentResponse STEP in Session - " + stepInSession.intValue());

        if (stepInSession == null) {
            return getAskSpeechletResponse(propertyReader.getWelcomeMessage(), propertyReader.getSpeechReprompt());
        }
        int iStepInSession = stepInSession.intValue();

        String newUserName =
                AddictionTreatmentFinderTextUtil.getUserName(intent.getSlot(SLOT_USER_NAME).getValue());
        if (newUserName == null) {
            String speechText = "I did not catch that. Can you tell me your name please?";
            return getAskSpeechletResponse(speechText, speechText);
        }

        log.debug("in getAddUserIntentResponse name is + " + newUserName);

        addictionUserData.setQuestionPhase(iStepInSession+1); //set the state of the next question
        session.setAttribute("SESSION-STEP", new Integer(addictionUserData.getQuestionPhase()));
        session.setAttribute("SESSION-USER-NAME", newUserName);
        addictionUserData.setfName(newUserName);

        log.debug("In getAddUserIntentResponse Addiction data - " + addictionUserData.toString());

        skillContext.setAddictionUserData(addictionUserData);
        return getAskSpeechletResponse(newUserName+", " + propertyReader.getQuestion2(), propertyReader.getQuestion2());
    }

    /**
     * Creates and returns response for the help intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the help intent
     */
    public SpeechletResponse getHelpIntentResponse(Intent intent, Session session,
                                                   SkillContext skillContext) {


        return skillContext.needsMoreHelp() ? getAskSpeechletResponse(propertyReader.getSpeechHelp(), propertyReader.getSpeechHelp())
                : getTellSpeechletResponse(propertyReader.getSpeechHelp());
    }

    /**
     * Creates and returns response for the exit intent.
     *
     * @param intent
     *            {@link Intent} for this request
     * @param session
     *            {@link Session} for this request
     * @param skillContext
     *            {@link SkillContext} for this request
     * @return response for the exit intent
     */
    public SpeechletResponse getExitIntentResponse(Intent intent, Session session,
                                                   SkillContext skillContext) {
        session.setAttribute("SESSION-STEP", new Integer(0));
        skillContext.setAddictionUserData(AddictionUserData.newInstance());

        return skillContext.needsMoreHelp() ? getTellSpeechletResponse(propertyReader.getGoodBye())
                : getTellSpeechletResponse("");
    }

    /**
     * Returns an ask Speechlet response for a speech and reprompt text.
     *
     * @param speechText
     *            Text for speech output
     * @param repromptText
     *            Text for reprompt output
     * @return ask Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getAskSpeechletResponse(String speechText, String repromptText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Session");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    /**
     * Returns a tell Speechlet response for a speech and reprompt text.
     *
     * @param speechText
     *            Text for speech output
     * @return a tell Speechlet response for a speech and reprompt text
     */
    private SpeechletResponse getTellSpeechletResponse(String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Session");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    private AddictionCentersData findCorrectCenter(List addictionCentersList, Session session) {
        AddictionCentersData correctAddictionCenter = null;
        String userMedicalChoice = (String) session.getAttribute("SESSION-USER-MEDICAL-CHOICE");
        log.debug(" The medican choice that the use selected was - " + userMedicalChoice);
        String twelveStep = "X";
        String medicallyAssisted = "X";

        if (userMedicalChoice.equalsIgnoreCase("12 step")) {
            twelveStep = "1";
        }

        for (int i=0; i<addictionCentersList.size(); i++) {
            correctAddictionCenter = (AddictionCentersData) addictionCentersList.get(i);
            log.debug(" Iterating - " + correctAddictionCenter.toString());
            if (correctAddictionCenter.getTwelveStep().equalsIgnoreCase(twelveStep)) {
                log.debug(" the correct addiction center is - " + correctAddictionCenter.toString());
                return correctAddictionCenter;
            }
        }

        log.debug(" None match and henve the selected addiction center is - " + correctAddictionCenter.toString());
        return correctAddictionCenter;
    }

    private AddictionCentersData unmarshall(String value) {
        String[] userDataArray = value.split(":");
        AddictionCentersData addictionCentersData = AddictionCentersData.newInstance();

        String name1 = userDataArray[0];
        String name2 = userDataArray[1];
        String street1 = userDataArray[2];
        String street2 = userDataArray[3];

        String city = userDataArray[4];

        String state = userDataArray[5];

        String zip = userDataArray[6];

        String zip4 = userDataArray[7];

        String county = userDataArray[8];

        String phoneNumber = userDataArray[9];

        String lgbt = userDataArray[10];

        String adolescents = userDataArray[11];

        String twelveStep = userDataArray[12];


        if (name1 == null) {name1 = ""; }
        if (name2 == null) {name2 = ""; }
        if (street1 == null) {street1 = ""; }
        if (street2 == null) {street2 = ""; }
        if (city == null) {city = ""; }
        if (state == null) {state = ""; }
        if (zip == null) {zip = ""; }
        if (zip4 == null) {zip4 = ""; }
        if (county == null) {county = ""; }
        if (phoneNumber == null) {phoneNumber = ""; }
        if (lgbt == null) {lgbt = ""; }
        if (adolescents == null) {adolescents = ""; }
        if (twelveStep == null) {twelveStep = ""; }

        addictionCentersData.setName1(name1);
        addictionCentersData.setName2(name2);
        addictionCentersData.setStreet1(street1);
        addictionCentersData.setStreet2(street2);
        addictionCentersData.setCity(city);
        addictionCentersData.setState(state);
        addictionCentersData.setZip(zip);
        addictionCentersData.setZip4(zip4);
        addictionCentersData.setCounty(county);
        addictionCentersData.setPhoneNumber(phoneNumber);
        addictionCentersData.setLgbt(lgbt);
        addictionCentersData.setAdolescents(adolescents);
        addictionCentersData.setTwelveStep(twelveStep);

        log.debug("in UnMarshal  - : " + addictionCentersData.toString());

        return addictionCentersData;
    }
}
