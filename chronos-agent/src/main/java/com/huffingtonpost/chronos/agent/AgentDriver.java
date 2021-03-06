package com.huffingtonpost.chronos.agent;

import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;
import org.joda.time.DateTime; 

import com.huffingtonpost.chronos.model.JobDao;
import com.huffingtonpost.chronos.model.JobSpec;
import com.huffingtonpost.chronos.model.PlannedJob; 

public class AgentDriver extends Stoppable {
  public static Logger LOG = Logger.getLogger(AgentDriver.class);
  
  private final JobDao dao;
  
  private final Thread me;
  
  private final Reporting reporting;
  
  public AgentDriver(JobDao dao, Reporting reporting) {
    this.dao = dao;
    this.reporting = reporting;
    me = new Thread(this);
  }

  public void init(){
    me.start();
  }
  
  public static boolean shouldJobRun(JobSpec aJob, DateTime now) {
    if (aJob.isEnabled() == false) {
      return false;
    }
    if (JobSpec.Interval.Hourly.equals(aJob.getInterval()) &&
        aJob.getStartMinute() == now.getMinuteOfHour()) {
      return true;
    } else if (JobSpec.Interval.Daily.equals(aJob.getInterval()) &&
               aJob.getStartHour() == now.getHourOfDay() &&
               aJob.getStartMinute() == now.getMinuteOfHour()) {
      return true;
    } else if (JobSpec.Interval.Weekly.equals(aJob.getInterval()) &&
               aJob.getStartDay() == now.getDayOfWeek() &&
               aJob.getStartHour() == now.getHourOfDay() &&
               aJob.getStartMinute() == now.getMinuteOfHour()) {
      return true;
    } else if (JobSpec.Interval.Monthly.equals(aJob.getInterval()) &&
               now.getDayOfMonth() == 1 &&
               aJob.getStartHour() == now.getHourOfDay() &&
               aJob.getStartMinute() == now.getMinuteOfHour()) {
      return true;
    }
    return false;
  }

  public void doRun() {
    DateTime now = Utils.getCurrentTime();
    List<JobSpec> jobs = dao.getJobs();
    for (JobSpec aJob : jobs) {
      if (shouldJobRun(aJob, now)) {
        LOG.info("Adding job to queue:" + aJob);
        dao.addToQueue(new PlannedJob(aJob, now));
      }
    }
    try {
      LOG.info(String.format("Sleeping for %d seconds...", (SLEEP_FOR / 1000)));
      Thread.sleep(SLEEP_FOR);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void run() {
    while (isAlive) {
      doRun();
    }
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    if (dao != null) {
      dao.close();
    } 
  }
 
}
