
package matching;

public class ReviewCompleted extends AbstractEvent {

    private Long matchId;
    private String review;

    public Long getMatchId() {
        return matchId;
    }
    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getReview() {
        return review;
    }
    public void setReview(String review) {
        this.review = review;
    }
}
