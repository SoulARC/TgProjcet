package tel.bot.streampostsbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "working_groups")
public class WorkingGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long channelId;
    private String nameChannel;
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Channel> channels;
    @ManyToOne()
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @PreRemove
    private void removeChannelsFromGroup(){
        for (Channel c: channels) {
            c.getWorkingGroups().remove(this);
        }
    }
}
