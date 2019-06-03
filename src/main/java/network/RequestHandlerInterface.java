package network;

import network.requests.*;

public interface RequestHandlerInterface {
    void handle(TellSuccessorToDeleteBackupRequest request);

    void handle(AskPredecessorBackupResourcesRequest request);

    void handle(FindSuccessorRequest request);

    void handle(DistributeResourceRequest request);

    void handle(AskSuccessorResourcesRequest request);

    void handle(UpdatePredecessorRequest request);

    void handle(UpdateSuccessorRequest request);

    void handle(FindSuccessorReplyRequest request);

    void handle(FixFingerRequest request);

    void handle(FixFingerReplyRequest request);

    void handle(LookupRequest request);

    void handle(LookupReplyRequest request);

    void handle(TransferAfterLeaveRequest request);

    void handle(PredecessorRequest request);

    void handle(PredecessorReplyRequest request);

    void handle(NotifyRequest request);

    void handle(GetResourceRequest request);

    void handle(GetResourceReply reply);
}
