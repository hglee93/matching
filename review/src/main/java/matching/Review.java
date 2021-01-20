package matching;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Review_table")
public class Review {

    @Id
    private Long matchId;
    private String review;
    private String status;

    @PostUpdate
    public void onPostUpdate() {
        if("ReviewCompleted".equals(status)) {
            ReviewCompleted reviewCompleted = new ReviewCompleted();
            reviewCompleted.setMatchId(this.matchId);
            reviewCompleted.setReview(this.review);
            reviewCompleted.publishAfterCommit();
        }
    }

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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }


}
