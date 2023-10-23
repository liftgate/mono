package io.liftgate.robotics.mono.pipeline

import io.liftgate.robotics.mono.Mono
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 10/22/2023
 */
interface SingleExecutionWithContext<T : StageContext> : SingleExecution
{
    fun provideContext(metadata: ExecutionMetadata): T
    fun executeAsync(level: Int, metadata: ExecutionMetadata, context: T)

    override fun execute(level: Int, metadata: ExecutionMetadata)
    {
        val start = System.currentTimeMillis()
        val context = provideContext(metadata)
        Mono.logSink("[${id()}] Created context")

        val future = CompletableFuture
            .runAsync({
                executeAsync(level, metadata, context)
                Mono.logSink("[${id()}] Finished async execution")
            }, Mono.EXECUTION)

        // wait until the future can does its thing
        Mono.logSink("[${id()}] Waiting until async execution is complete...")
        if (context.requiresAtLeast() != null)
        {
            Thread.sleep(context.requiresAtLeast()!!)
        } else
        {
            future.join()
        }

        while (!context.isCompleted())
        {
            if (context.timesOutAfter() != null)
            {
                if (System.currentTimeMillis() >= start + context.timesOutAfter()!!)
                {
                    if (!future.isDone)
                    {
                        // cancel the future if it's doing some stuff
                        future.cancel(true)
                    }

                    Mono.logSink("[${id()}] Timed out, continuing")
                    break
                }
            }

            // block until it's complete
            Thread.sleep(50L)
        }

        context.dispose()
        Mono.logSink("[${id()}] Disposed of context")
    }
}
