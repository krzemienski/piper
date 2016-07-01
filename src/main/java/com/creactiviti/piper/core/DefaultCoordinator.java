package com.creactiviti.piper.core;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DefaultCoordinator implements Coordinator {

  @Autowired private Messenger messenger;
  @Autowired private PipelineFactory pipelineFactory;
  @Autowired private JobRepository jobRepository;
  @Autowired private ApplicationEventPublisher eventPublisher;
 
  private Logger log = LoggerFactory.getLogger(getClass());
  
  private static final String PIPELINE_ID = "pipelineId";
  
  @Override
  public Job start (Map<String, Object> aInput) {
    String pipelineId = (String) aInput.get(PIPELINE_ID);
    Assert.notNull(pipelineId,String.format("Missing mandatory parameter %s", PIPELINE_ID));
    Pipeline pipeline = pipelineFactory.createPipeline(pipelineId);
    Assert.notNull(pipeline,String.format("Unkown pipeline: %s", pipelineId));
    SimpleJob job = new SimpleJob(pipeline);
    job.setStatus(JobStatus.STARTED);
    log.debug("Job {} started",job.getId());
    jobRepository.save(job);
    run(job);
    return job;
  }
  
  private void run (Job aJob) {
    Pipeline pipeline = aJob.getPipeline();
    if(pipeline.hasNextTask()) {
      MutableTask nextTask = new MutableTask(pipeline.nextTask().toMap());
      nextTask.setJobId(aJob.getId());
      messenger.send(nextTask.getNode(), nextTask);
    }
    else {
      aJob.setStatus(JobStatus.COMPLETED);
      jobRepository.save(aJob);
      log.debug("Job {} completed successfully",aJob.getId());
    }
  }

  @Override
  public Job stop (String aJobId) {
    return null;
  }

  @Override
  public Job resume (String aJobId) {
    return null;
  }

  @Override
  public Job get (String aJobId) {
    return null;
  }

  @Override
  public void complete (Task aTask) {
    log.debug("Completing {}", aTask);
    String jobId = aTask.getJobId();
    Job job = jobRepository.find(jobId);
    Assert.notNull(job,String.format("Unknown Job %s ",jobId));
    run(job);
  }

  @Override
  public void error (Task aTask) {
  }

  @Override
  public void on (Object aEvent) {
    eventPublisher.publishEvent (aEvent);    
  }

}