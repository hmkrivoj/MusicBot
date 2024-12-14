/*
 * Copyright 2018 John Grosh (jagrosh)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Prompt
{
    private final String title;

    private boolean noprompt;
    private Scanner scanner;
    
    public Prompt(String title)
    {
        this(title, "true".equalsIgnoreCase(System.getProperty("noprompt")));
    }
    
    public Prompt(String title, boolean noprompt)
    {
        this.title = title;
        this.noprompt = noprompt;
    }
    
    public void alert(Level level, String context, String message)
    {
        Logger log = LoggerFactory.getLogger(context);
        switch(level)
        {
            case INFO:
                log.info(message);
                break;
            case WARNING:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            default:
                log.info(message);
                break;
        }
    }
    
    public String prompt(String content)
    {
        if(noprompt)
            return null;
        if (scanner == null)
            scanner = new Scanner(System.in);
        try
        {
            System.out.println(content);
            if(scanner.hasNextLine())
                return scanner.nextLine();
            return null;
        }
        catch(Exception e)
        {
            alert(Level.ERROR, title, "Unable to read input from command line.");
            e.printStackTrace();
            return null;
        }
    }
    
    public enum Level
    {
        INFO, WARNING, ERROR;
    }
}
