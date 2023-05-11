package edu.touro.mco152.bm.Commands;

/**
 * This class is passed in an instance of the CommandInterface and executes its execute method
 */
public class Executor {
    public boolean executeCommand(CommandInterface cmd){
       return cmd.execute();
    }
}
