package com.jonasmp.ai.threat;

import com.jonasmp.ai.bootstrap.CoreBootstrap;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ThreatDecayTask {
   public void start() {
      (new BukkitRunnable() {
         {
            Objects.requireNonNull(ThreatDecayTask.this);
         }

         public void run() {
            ThreatScoreManager manager = CoreBootstrap.THREAT_MANAGER;
            Bukkit.getOnlinePlayers().forEach(player -> {
               double current = manager.getScore(player.getUniqueId());
               if (current > 0.0) {
                  double decay = ThreatDecayTask.this.calculateDecay(current);
                  manager.reduceThreat(player.getUniqueId(), decay);
               }
            });
         }
      }).runTaskTimer(CoreBootstrap.PLUGIN, 1200L, 1200L);
   }

   private double calculateDecay(double score) {
      if (score > 80.0) {
         return 1.0;
      } else if (score > 60.0) {
         return 2.0;
      } else {
         return score > 30.0 ? 3.0 : 4.0;
      }
   }
}
