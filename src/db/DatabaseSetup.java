package db;


import main.Starter;
import util.Printer;
import util.Utility;

import java.sql.ResultSet;

public class DatabaseSetup {

    public static void checkSetup() {
        Printer.printToLog("Checking database table integrity and creating new tables if necessary...", Printer.LOGTYPE.INFO);

        if(!checkForTable("curr_bal"))createCurrentBalancesTable();
        if(!checkForTable("curr_trades"))createCurrentTradesTable();
        if(!checkForTable("history_trades"))createHistoryTradesTable();
        if(!checkForTable("history_options"))createHistoryOptionsTable();
        if(!checkForTable("history_deliveries"))createHistoryDeliveriesTable();

        Printer.printToLog("Database is all set up and ready!", Printer.LOGTYPE.INFO);
    }


    private static boolean checkForTable(String tableName) {
        boolean result = false;
        try{
            ResultSet rs = Starter.db.runQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + Starter.db_schema + "' AND TABLE_NAME = '" + tableName + "';");
            result = Utility.getSize(rs) > 0;
        } catch (Exception e){
            e.printStackTrace();
            System.exit(2);
        }
        return result;
    }

    private static void createCurrentTradesTable(){
        Starter.db.runInsert("CREATE TABLE `" + Starter.db_schema + "`.`curr_trades` (\n" +
                "  `instrument_name` VARCHAR(50) NOT NULL,\n" +
                "  `currency` VARCHAR(3) NOT NULL,\n" +
                "  `expiry_date` VARCHAR(45) NOT NULL,\n" +
                "  `strike_price` INT NOT NULL,\n" +
                "  `kind` VARCHAR(4) NOT NULL,\n" +
                "  `status` VARCHAR(45) NOT NULL,\n" +
                "  `time_initially` VARCHAR(45) NOT NULL,\n" +
                "  `time_remaining` VARCHAR(45) NOT NULL,\n" +
                "  `open_position` VARCHAR(7) NOT NULL,\n" +
                "  `current_value` DOUBLE NOT NULL,\n" +
                "  `current_value_usd` DOUBLE NOT NULL,\n" +
                "  `max_possible_value` DOUBLE NOT NULL,\n" +
                "  `max_possible_value_usd` DOUBLE NOT NULL,\n" +
                "  PRIMARY KEY (`instrument_name`))\n" +
                "ENGINE = InnoDB\n" +
                "DEFAULT CHARACTER SET = utf8mb4\n" +
                "COLLATE = utf8mb4_unicode_ci;");
    }

    private static void createCurrentBalancesTable(){
        Starter.db.runInsert("CREATE TABLE `" + Starter.db_schema + "`.`curr_bal` (\n" +
                "  `hash` VARCHAR(64) NOT NULL,\n" +
                "  `btc_current_balance` DOUBLE NOT NULL,\n" +
                "  `btc_current_balance_usd` DOUBLE NOT NULL,\n" +
                "  `btc_open_trades_max_value` DOUBLE NOT NULL,\n" +
                "  `btc_open_trades_max_value_usd` DOUBLE NOT NULL,\n" +
                "  `btc_open_trades_current_value` DOUBLE NOT NULL,\n" +
                "  `btc_open_trades_current_value_usd` DOUBLE NOT NULL,\n" +
                "  `btc_longest_trade_expiry_date` VARCHAR(45) NOT NULL,\n" +
                "  `eth_current_balance` DOUBLE NOT NULL,\n" +
                "  `eth_current_balance_usd` DOUBLE NOT NULL,\n" +
                "  `eth_open_trades_max_value` DOUBLE NOT NULL,\n" +
                "  `eth_open_trades_max_value_usd` DOUBLE NOT NULL,\n" +
                "  `eth_open_trades_current_value` DOUBLE NOT NULL,\n" +
                "  `eth_open_trades_current_value_usd` DOUBLE NOT NULL,\n" +
                "  `eth_longest_trade_expiry_date` VARCHAR(45) NOT NULL,\n" +
                "  `total_current_balance_usd` DOUBLE NOT NULL,\n" +
                "  PRIMARY KEY (`hash`))\n" +
                "ENGINE = InnoDB\n" +
                "DEFAULT CHARACTER SET = utf8mb4\n" +
                "COLLATE = utf8mb4_unicode_ci;");
    }

    private static void createHistoryTradesTable(){

    }

    private static void createHistoryOptionsTable(){

    }

    private static void createHistoryDeliveriesTable(){

    }
}
