import ballerina/lang.'string;
import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

const START_POSITION_SUBJECT_NAME = "nats-streaming-start-position";

string receivedStartPositionFirstMessages = "";
string receviedStartPositionLastReceivedMessages = "";
string receivedStartPositionTimeDeltaMessages = "";
string receivedStartPositionSequenceNumberMessages = "";

@test:Config {
    groups: ["nats-streaming"]
}
function testConsumerServicesWithHistoricalMessageReplay() returns error? {
    Listener sub = check new(DEFAULT_URL);
    Client newClient = check new(DEFAULT_URL);
    check sub.attach(startPositionFirstService);
    check sub.attach(startPositionLastReceivedService);
    check sub.attach(startPositionTimeDeltaService);
    check sub.attach(startPositionSequenceNumberService);

    string firstMessage = "1";
    string middleMessage = "2";
    string lastMessage = "3";
    string afterMessage = "4";

    future<string|error> id1  = start newClient->publishMessage({ content: firstMessage.toBytes(),
                                                                  subject: START_POSITION_SUBJECT_NAME });
    string|error id = wait id1;
    runtime:sleep(10);
    future<string|error> id2 = start newClient->publishMessage({ content: middleMessage.toBytes(),
                                                                 subject: START_POSITION_SUBJECT_NAME });
    id = wait id2;
    future<string|error> id3 = start newClient->publishMessage({ content: lastMessage.toBytes(),
                                                                 subject: START_POSITION_SUBJECT_NAME });
    id = wait id3;
    check sub.'start();
    runtime:sleep(10);
    id = check newClient->publishMessage({ content: afterMessage.toBytes(),
                                                subject: START_POSITION_SUBJECT_NAME });
    runtime:sleep(5);
    test:assertEquals(receivedStartPositionFirstMessages, firstMessage + middleMessage + lastMessage + afterMessage,
                      msg = "(FIRST) Message received does not match.");
    test:assertEquals(receviedStartPositionLastReceivedMessages, lastMessage + afterMessage,
                      msg = "(LAST RECEIVED) Message received does not match.");
    test:assertEquals(receivedStartPositionTimeDeltaMessages, middleMessage + lastMessage + afterMessage,
                      msg = "(TIME DELTA) Message received does not match.");
    test:assertEquals(receivedStartPositionSequenceNumberMessages, middleMessage + lastMessage + afterMessage,
                      msg = "(SEQUENCE NUMBER) Message received does not match.");

    check newClient.close();
    check sub.close();
}

Service startPositionLastReceivedService =
@ServiceConfig {
    subject: START_POSITION_SUBJECT_NAME,
    startPosition: LAST_RECEIVED
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receviedStartPositionLastReceivedMessages += messageContent;
            log:printInfo("Message Received (LAST_RECEIVED): " + messageContent);
        }
    }
};

Service startPositionFirstService =
@ServiceConfig {
    subject: START_POSITION_SUBJECT_NAME,
    startPosition: FIRST
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedStartPositionFirstMessages += messageContent;
            log:printInfo("Message Received (FIRST): " + messageContent);
        }
    }
};

Service startPositionTimeDeltaService =
@ServiceConfig {
    subject: START_POSITION_SUBJECT_NAME,
    startPosition: [TIME_DELTA_START, 5]
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedStartPositionTimeDeltaMessages += messageContent;
            log:printInfo("Message Received (TIME DELTA): " + messageContent);
        }
    }
};

Service startPositionSequenceNumberService =
@ServiceConfig {
    subject: START_POSITION_SUBJECT_NAME,
    startPosition: [SEQUENCE_NUMBER, 2]
}
service object {
    remote function onMessage(Message msg, Caller caller) {
        string|error messageContent = 'string:fromBytes(msg.content);
        if (messageContent is string) {
            receivedStartPositionSequenceNumberMessages += messageContent;
            log:printInfo("Message Received (SEQUENCE NUMBER): " + messageContent);
        }
    }
};
