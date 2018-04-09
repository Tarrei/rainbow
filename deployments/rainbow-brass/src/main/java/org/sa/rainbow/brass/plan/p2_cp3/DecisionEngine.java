package org.sa.rainbow.brass.plan.p2_cp3;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sa.rainbow.brass.PropertiesConnector;
import org.sa.rainbow.brass.model.map.EnvMap;
import org.sa.rainbow.brass.plan.p2_cp3.MapTranslator;
import org.sa.rainbow.brass.adaptation.PrismPolicy;
import org.sa.rainbow.brass.adaptation.PrismConnectorAPI;
import org.sa.rainbow.brass.adaptation.PolicyToIG;

import org.sa.rainbow.brass.confsynthesis.ConfigurationProvider;
import org.sa.rainbow.brass.confsynthesis.ConfigurationSynthesizer;
import org.sa.rainbow.brass.confsynthesis.SimpleConfigurationStore;


import com.google.common.base.Objects;





/**
 * @author jcamara
 *
 */
public class DecisionEngine {
	public enum ChallengeProblemMode {
	    CP1, CP3;
	}
    public static String m_export_path;
    public static MapTranslator m_mt;
    public static String m_origin;
    public static String m_destination;
    public static Map<List, String> m_candidates;
    public static Map<List, ArrayList<Double>> m_scoreboard;
    public static double m_selected_candidate_time;
    public static double m_selected_candidate_safety;
    public static double m_selected_candidate_energy;
    public static double m_selected_candidate_score;
    public static PrismPolicy m_plan;
    public static ChallengeProblemMode m_mode = ChallengeProblemMode.CP3;

    public static final double INFINITY = 999999.0;

    /**
     * Initializes decision engine
     * @param props
     */
    public static void init (Properties props) throws Exception {
        if (props == null) {
            props = PropertiesConnector.DEFAULT;
        }
        m_export_path = props.getProperty (PropertiesConnector.PRISM_OUTPUT_DIR_PROPKEY);
        m_export_path = m_export_path.replaceAll ("\\\"", "");
        m_mt = new MapTranslator ();
        new PrismConnectorAPI (); // PRISM invoked via API
        m_origin="";
        m_destination="";
        m_selected_candidate_time=0.0;
        m_selected_candidate_safety=0.0;
        m_selected_candidate_score=0.0;
        m_scoreboard= new HashMap<List, ArrayList<Double>>();
    }

    /**
     * Sets the map to extract data 
     * @param map
     */
    public static void setMap(EnvMap map){
        m_mt.setMap(map);
    }
    
    /**
     * Sets the configuration provider to extract data
     * @param cp
     */
    public static void setConfigurationProvider(ConfigurationProvider cp){
        m_mt.setConfigurationProvider(cp);
    }
    
    public static void setChallengeProblemMode (ChallengeProblemMode m){
    	m_mode = m;
    }

    /**
     * Generates all PRISM specifications corresponding to the different non-cyclic paths between
     * origin and destination locations
     * @param origin String label of origin map location
     * @param destination String label of destination map location
     */

    public static void generateCandidates (String origin, String destination){
        generateCandidates(origin, destination, false);
    }

    public static void generateCandidates(String origin, String destination, boolean inhibitTactics){
        m_origin = origin;
        m_destination = destination;
        m_candidates = m_mt.exportConstrainedTranslationsBetweenCutOff(m_export_path, origin, destination, inhibitTactics);	
//        m_candidates = m_mt.exportConstrainedTranslationsBetween(m_export_path, origin, destination, inhibitTactics);	
//        m_candidates = m_mt.exportSingleTranslationBetween(m_export_path, origin, destination, inhibitTactics);	
    }

    /**
     * Assings a score to each one of the candidate policies synthesized based on the specifications generated by
     * generateCandidates
     * 
     * @param map
     * @param batteryLevel
     *            String amount of remaining battery
     * @param robotHeading
     *            String robot Heading (needs to be converted to an String encoding an int from MissionState.Heading)
     * @throws Exception
     */
    public static void scoreCandidates (EnvMap map, String batteryLevel, String robotHeading) throws Exception {
        m_scoreboard.clear();
        String propFileString = "/mapbotp2cp3.props";
        if (m_mode==ChallengeProblemMode.CP1){
        	propFileString = "/mapbotp2cp1.props";
        }
        
        synchronized (map){
            String m_consts = MapTranslator.INITIAL_ROBOT_CONF_CONST+"=-1,"+MapTranslator.INITIAL_ROBOT_LOCATION_CONST+"="+String.valueOf(map.getNodeId(m_origin)) +","+ MapTranslator.TARGET_ROBOT_LOCATION_CONST 
                    + "="+String.valueOf(map.getNodeId(m_destination))+ "," + MapTranslator.INITIAL_ROBOT_BATTERY_CONST+"="+batteryLevel+","+MapTranslator.INITIAL_ROBOT_HEADING_CONST+"="+robotHeading;

            System.out.println(m_consts);
            String result;
            for (List candidate_key : m_candidates.keySet() ){                           	
                result = PrismConnectorAPI.modelCheckFromFileS (m_candidates.get (candidate_key), m_export_path + propFileString, 
                		m_candidates.get (candidate_key), -1, m_consts);
                
                String[] results = result.split(",");
                ArrayList<Double> resultItems = new ArrayList<Double>();
                for (int i=0; i<results.length; i++){
                	if (!Objects.equal(results[i], "Infinity")) {
                		resultItems.add(Double.valueOf(results[i]));
                	}
                	else {
                		resultItems.add(INFINITY);
                	}
                }
        		m_scoreboard.put(candidate_key, resultItems);
            }
        }
    }

    
    public static Double getMaxItem(int index){
    	Double res = 0.0;
    	for (Map.Entry<List, ArrayList<Double>> entry : m_scoreboard.entrySet()){
    		if (entry.getValue().get(index)>res){
    			res = entry.getValue().get(index);
    		}
    	}
    	return res;
    }
    
    public static Double getMaxTimeCP3(){
    	return getMaxItem(0);
    }    
    
    
    public static String selectPolicy(){
    	if (m_mode==ChallengeProblemMode.CP1){
    		return selectPolicyCP1();
    	}
    		return selectPolicyCP3();
    }
    
    
    /**
     * Selects the policy with the best score (CP3)
     * @return String filename of the selected policy
     */
    public static String selectPolicyCP3(){
    	
    	Double m_safetyWeight=0.5;
    	Double m_timelinessWeight=0.5;
    	
    	Double maxTime = getMaxTimeCP3();
    	Double maxScore=0.0;
    	
        Map.Entry<List, ArrayList<Double>> maxEntry = m_scoreboard.entrySet().iterator().next();
        for (Map.Entry<List, ArrayList<Double>> entry : m_scoreboard.entrySet())
        {
            Double entryTime = entry.getValue().get(0);
            Double entryTimeliness = 0.0;
            if (maxTime>0.0){
            	entryTimeliness = 1.0-(entryTime / maxTime);
            }
            Double entrySafety = entry.getValue().get(1);
            
            
            Double entryScore = m_safetyWeight * entrySafety + m_timelinessWeight * entryTimeliness;
            
        	if ( entryScore > maxScore)
            {
                maxEntry = entry;
                maxScore = entryScore;
            }
        }
        m_selected_candidate_time = maxEntry.getValue().get(0);
        m_selected_candidate_safety = maxEntry.getValue().get(1);
        m_selected_candidate_score = maxScore;
        
        System.out.println("Selected candidate policy: "+m_candidates.get(maxEntry.getKey()));
        System.out.println("Score: "+String.valueOf(m_selected_candidate_score)+" Safety: "+String.valueOf(m_selected_candidate_safety)+" Time: "+String.valueOf(m_selected_candidate_time));
        
        return m_candidates.get(maxEntry.getKey())+".adv";
    }

    public static double getSelectedPolicyTime(){
        return m_selected_candidate_time;
    }

    
    public static Double getMaxTimeCP1(){
    	return getMaxItem(0);
    }

    public static Double getMaxEnergyCP1(){
    	return getMaxItem(1);
    }
    
    /**
     * Selects the policy with the best score (CP1)
     * @return String filename of the selected policy
     */
    public static String selectPolicyCP1(){
    	
    	Double m_energyWeight=0.5;
    	Double m_timelinessWeight=0.5;
    	
    	Double maxTime = getMaxTimeCP1();
    	Double maxEnergy = getMaxEnergyCP1();
    	Double maxScore=0.0;
    	
        Map.Entry<List, ArrayList<Double>> maxEntry = m_scoreboard.entrySet().iterator().next();
        for (Map.Entry<List, ArrayList<Double>> entry : m_scoreboard.entrySet())
        {
            Double entryTime = entry.getValue().get(0);
            Double entryTimeliness = 0.0;
            if (maxTime>0.0){
            	entryTimeliness = 1.0-(entryTime / maxTime);
            }
            
            Double entryEnergy = entry.getValue().get(1)/maxEnergy;
            
            
            Double entryScore = m_energyWeight * entryEnergy + m_timelinessWeight * entryTimeliness;
            
        	if ( entryScore > maxScore)
            {
                maxEntry = entry;
                maxScore = entryScore;
            }
        }
        m_selected_candidate_time = maxEntry.getValue().get(0);
        m_selected_candidate_energy = maxEntry.getValue().get(1);
        m_selected_candidate_score = maxScore;
        
        System.out.println("Selected candidate policy: "+m_candidates.get(maxEntry.getKey()));
        System.out.println("Score: "+String.valueOf(m_selected_candidate_score)+" Energy: "+String.valueOf(m_selected_candidate_energy)+" Time: "+String.valueOf(m_selected_candidate_time));
        
        return m_candidates.get(maxEntry.getKey())+".adv";
    }

    
    
    /**
     * Class test
     * @param args
     */
    public static void main (String[] args) throws Exception {
        init (null);

        List<Point2D> coordinates = new ArrayList<Point2D>();
        PrismPolicy pp=null;

        setChallengeProblemMode(ChallengeProblemMode.CP3);

        EnvMap dummyMap = new EnvMap (null, null);
        System.out.println("Loading Map: "+PropertiesConnector.DEFAULT.getProperty(PropertiesConnector.MAP_PROPKEY));
        dummyMap.loadFromFile(PropertiesConnector.DEFAULT.getProperty(PropertiesConnector.MAP_PROPKEY));
        System.out.println("Setting map...");
        setMap(dummyMap);
        
        ConfigurationSynthesizer cs = new ConfigurationSynthesizer();
//        SimpleConfigurationStore cs = new SimpleConfigurationStore();
        System.out.println("Populating configuration list..");
        cs.populate();
        System.out.println("Setting configuration provider...");
        setConfigurationProvider(cs);
        
        
        for (int i=32000; i< 32500; i+=500){
        	System.out.println("Generating candidates for l1-l4...");
            generateCandidates("l1", "l4");
        	System.out.println("Scoring candidates...");
            scoreCandidates(dummyMap, String.valueOf(i), "1");
            System.out.println(String.valueOf(m_scoreboard));	        
            pp = new PrismPolicy(selectPolicy());
            pp.readPolicy();  
            String plan = pp.getPlan().toString();
            System.out.println(plan);
            PolicyToIG translator = new PolicyToIG(pp, dummyMap);
            System.out.println (translator.translate (20394, false));
            coordinates.add(new Point2D.Double(i, m_selected_candidate_time));
        }

        for (int j=0; j< coordinates.size(); j++){
            System.out.println(" ("+String.valueOf(coordinates.get(j).getX())+", "+String.valueOf(coordinates.get(j).getY())+") ");
        }

    }

}