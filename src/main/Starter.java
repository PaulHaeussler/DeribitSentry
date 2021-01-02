package main;


import com.google.gson.internal.LinkedTreeMap;
import db.DBThread;
import db.Database;
import db.DatabaseSetup;
import movements.Moment;
import movements.Movement;
import movements.Option;
import movements.Trade;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static main.ApiController.CURRENCY.BTC;
import static main.ApiController.CURRENCY.ETH;
import static util.Printer.LOGTYPE.INFO;

public class Starter {

    public static String repo = "";

    private static final long MILLIES_INTERVAL = 5000;
    private static final double MAX_PRICE_INCREASE_MULIPLIER = 3; //= 300% loss
    private static final double HARD_STOP_BTC_LOSS = -0.05;
    private static final double HARD_STOP_ETH_LOSS = -2.0;

    private static ApiController api;
    public static Database db;
    public static String db_schema = "";

    public static HashMap<String, String> userMappingsBTC;
    public static HashMap<String, String> userMappingsETH;

    public static String[] a;

    public static void main(String[] args){
        a = args;

        Printer.checkSetup("DeribitSentry");
        HashMap<String, String> argmap = Utility.checkStartupArgs(args);
        repo = argmap.get("repo");
        db_schema = argmap.get("dbname");
        Printer.printToLog("Sentry starting up...", INFO);
        db = new Database(argmap.get("user"), argmap.get("pw"), argmap.get("host"), argmap.get("dbname"));
        DatabaseSetup.checkSetup();

        api = new ApiController();
        api.authenticate(new Credentials(repo + "/api.key"));
        ApiController.userMappingsBTC = Movement.getUserMappings(repo + "/users_btc.mapping");
        ApiController.userMappingsETH = Movement.getUserMappings(repo + "/users_eth.mapping");

        Printer.printToLog("Sentry operational!", INFO);

        while(true){
            try{
                long start = System.currentTimeMillis();
                TreeMap<Double, Moment> hb = ApiController.compileTradeList(api,true, BTC);
                TreeMap<Double, Moment> he = ApiController.compileTradeList(api,true, ETH);


                for(Moment moment : hb.values()){
                    if(moment.movement instanceof Trade){
                        if(((Trade) moment.movement).state == Option.STATE.OPEN){
                            evaluatePosition(moment);
                        }
                    }
                }

                for(Moment moment : he.values()){
                    if(moment.movement instanceof Trade){
                        if(((Trade) moment.movement).state == Option.STATE.OPEN){
                            evaluatePosition(moment);
                        }
                    }
                }

                Runnable r = new DBThread(hb, he, api.getIndex(ApiController.CURRENCY.BTC), api.getIndex(ApiController.CURRENCY.ETH));
                Thread t = new Thread(r);
                t.start();

                long timeDiff = System.currentTimeMillis() - start;
                if(timeDiff < MILLIES_INTERVAL) Thread.sleep(MILLIES_INTERVAL - timeDiff);
            } catch (Exception e){
                Printer.printException(e);
                System.exit(1);
            }
        }
    }

    private static void evaluatePosition(Moment pos) throws Exception {
        Trade t = (Trade) pos.movement;
        if(t.openPos < 0){


            t.maxPrice = t.avgPrem * MAX_PRICE_INCREASE_MULIPLIER;


            double stopLoss = 1000.0;
            if(t.currency == BTC){
                stopLoss = HARD_STOP_BTC_LOSS;
            } else if(t.currency == ETH){
                stopLoss = HARD_STOP_ETH_LOSS;
            }
            boolean criteriaA = t.diffToStrike < 0;
            boolean criteriaB = t.maxPrice < t.ask;
            boolean criteriaC = (t.currValue < stopLoss);

            if(criteriaA || criteriaB || criteriaC){
                Printer.printToLog("A: " + criteriaA + ", B: " + criteriaB + ", C: " + criteriaC, INFO);
                if(Utility.noKill) return;
                Printer.printToLog("Killing " + t.instrumentName, INFO);
                api.killPosition(t);

            }
        } else {


        }
    }



    public static HashMap<String, String> getUserMappingByCurrency(ApiController.CURRENCY currency){
        if(currency == BTC){
            return userMappingsBTC;
        } else if(currency == ETH) {
            return userMappingsETH;
        }
        return null;
    }
}
