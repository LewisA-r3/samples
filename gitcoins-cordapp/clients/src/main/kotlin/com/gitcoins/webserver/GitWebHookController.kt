package com.gitcoins.webserver

import com.gitcoins.flows.CreateKeyFlow
import com.gitcoins.flows.PullRequestReviewEventFlow
import com.gitcoins.flows.PushEventFlow
import com.gitcoins.jsonparser.ResponseParser
import net.corda.core.flows.FlowException
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints to be called by GitHub webhooks.
 */
@RestController
@RequestMapping("/api/git/")
class GitWebHookController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * End point that should be called by a 'pull_request_review_comments' webhook.
     */
    @PostMapping(value = ["/create-key"])
    fun createKey(@RequestBody msg: String): ResponseEntity<String> {

        val isCreate = ResponseParser.verifyCreateKey(msg)
        if (!isCreate) {
            return ResponseEntity.badRequest().body("Invalid pr comment. Please comment 'createKey'.")
        }

        val gitUserName = ResponseParser.extractGitHubUsername(".*comment.*user.*login.*", msg)

        return when (gitUserName) {
            null -> ResponseEntity.badRequest().body("Github username must be present.\n")
            else -> try {
                proxy.startTrackedFlow(::CreateKeyFlow, gitUserName).returnValue.getOrThrow()
                ResponseEntity.status(HttpStatus.CREATED).body("New public key generated for github user: $gitUserName")
            } catch (ex: FlowException) {
                ResponseEntity.badRequest().body("Could not create key for Git user: $gitUserName")
            }
        }
    }


    /**
     * End point that should be called by a 'push' webhook.
     */
    @PostMapping(value = ["/push-event"])
    fun initPushFlow(@RequestBody msg: String): ResponseEntity<String> {

        val gitUserName = ResponseParser.extractGitHubUsername(".*pusher.*name.*", msg)

        return when (gitUserName) {
            null -> ResponseEntity.badRequest().body("Github username must be present.\n")
            else -> try {
                proxy.startTrackedFlow(::PushEventFlow, gitUserName).returnValue.getOrThrow()
                ResponseEntity.status(HttpStatus.CREATED).body("New public key generated for github user: $gitUserName")
            } catch (ex: FlowException) {
                ResponseEntity.badRequest().body("Could not create key for Git user: $gitUserName")
            }
        }
    }


    /**
     * End point that should be called by a 'pull_request_review' webhook.
     */
    @PostMapping(value = ["/pr-event"])
    fun initPRFlow(@RequestBody msg: String): ResponseEntity<String> {

        val gitUserName = ResponseParser.extractGitHubUsername(".*review.*user.*login.*", msg)

        return when (gitUserName) {
            null -> ResponseEntity.badRequest().body("Github username must be present.\n")
            else -> try {
                proxy.startTrackedFlow(::PullRequestReviewEventFlow, gitUserName).returnValue.getOrThrow()
                ResponseEntity.status(HttpStatus.CREATED).body("New public key generated for github user: $gitUserName")
            } catch (ex: FlowException) {
                ResponseEntity.badRequest().body("Could not create key for Git user: $gitUserName")
            }
        }
    }
}