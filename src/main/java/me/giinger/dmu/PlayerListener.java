package me.giinger.dmu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {

    Random gen = new Random();
    int i = 0;
    public final Drugs plugin;

    public PlayerListener(Drugs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        if (player.hasPermission("drugs.use") || player.isOp()) {
            ItemStack stack = player.getItemInHand();
            if (stack != null) {
                short data = stack.getDurability();
                if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e
                        .getAction().equals(Action.RIGHT_CLICK_BLOCK))
                        && (player.isSneaking())) {
                    if (plugin.isDrug(stack.getTypeId(), data)) {
                        ItemStack old = new ItemStack(e.getPlayer()
                                .getItemInHand().getTypeId(), e.getPlayer()
                                .getItemInHand().getAmount() - 1, data);
                        e.getPlayer().setItemInHand(old);
                        gatherEffects(player, stack.getTypeId(), data);
                        plugin.getNoPlace().add(player.getName());
                        doSmoke(player, stack.getTypeId(), data);
                        if (plugin.config
                                .getBoolean("Options.EnableNegativeEffects") == true) {
                            doNegatives(player, stack.getTypeId(), data);
                        }

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                                new Runnable() {
                            public void run() {
                                plugin.getNoPlace().remove(player.getName());
                            }
                        }, 20);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (plugin.getIsJump().contains(((Player) e.getEntity()).getName())) {
                if (e.getCause().equals(DamageCause.FALL)) {
                    if (plugin.config
                            .getBoolean("Options.EnableJumpProtection") == true) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (plugin.getIsJump().contains(e.getPlayer().getName())) {
            String initial = e.getMessage();
            String end = scramble(initial);
            e.setMessage(end);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) throws IOException {
        if (plugin.getIsUpdate()) {
            if (e.getPlayer().hasPermission("drugs.updates")
                    || e.getPlayer().isOp()) {
                e.getPlayer()
                        .sendMessage(
                        ChatColor.RED
                        + "*\n* [DrugMeUp] Update Available! \n* Download it at: dev.bukkit.org/server-mods/drugmeup\n*");
            }
        }
    }

    public void doSmoke(Player p, int id, short dmg) {
        boolean smoke;
        if (dmg == 0) {
            smoke = plugin.config.getBoolean("DrugIds." + id + ".Smoke");
        } else {
            smoke = plugin.config.getBoolean("DrugIds." + id + ":" + dmg
                    + ".Smoke");
        }
        if (smoke) {
            for (int iSmoke = 0; iSmoke <= 8; iSmoke++) {
                p.getWorld().playEffect(p.getLocation(), Effect.SMOKE, iSmoke);
            }
        }
    }

    public void doNegatives(Player p, int id, short dmg) {
        List<Integer> negatives = new ArrayList<Integer>();
        try {
            if (dmg == 0) {
                String[] negs = plugin.config
                        .getString("DrugIds." + id + ".Negatives")
                        .replaceAll(" ", "").split(",");
                for (String s : negs) {
                    negatives.add(Integer.parseInt(s));
                }
            } else {
                String[] negs = plugin.config
                        .getString("DrugIds." + id + ":" + dmg + ".Negatives")
                        .replaceAll(" ", "").split(",");
                for (String s : negs) {
                    negatives.add(Integer.parseInt(s));
                }
            }
            if (negatives.contains(0)) {
                return;
            } else {
                int iNegative = gen.nextInt(32);
                if (iNegative < 1) {
                    iNegative = 5;
                }
                if (iNegative > 32) {
                    iNegative = 32;
                }

                if (iNegative == 1) {
                    if (negatives.contains(1)) {
                        pukeInv(p);
                    }
                } else if (iNegative == 2) {
                    if (negatives.contains(2)) {
                        torchYa(p);
                    }
                } else if (iNegative == 3) {
                    if (negatives.contains(3)) {
                        heartAttack(p);
                    }
                } else if (iNegative == 4) {
                    if (negatives.contains(4)) {
                        youOd(p);
                    }
                }
            }
            negatives.clear();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void gatherEffects(Player p, int i, short dmg) {
        plugin.getEffects(i, dmg);

        if (dmg == 0) {
            if (plugin.config.getString("DrugIds." + i + ".Type")
                    .equalsIgnoreCase("All")) {
                for (int ii : plugin.getEffectList()) {
                    applyEffect(p, ii);
                }
                plugin.getEffectList().clear();
            } else if (plugin.config.getString("DrugIds." + i + ".Type")
                    .equalsIgnoreCase("Random")) {
                doRandomEffects(p);
            }
        } else {
            if (plugin.config.getString("DrugIds." + i + ":" + dmg + ".Type")
                    .equalsIgnoreCase("All")) {
                for (int ii : plugin.getEffectList()) {
                    applyEffect(p, ii);
                }
                plugin.getEffectList().clear();
            } else if (plugin.config.getString(
                    "DrugIds." + i + ":" + dmg + ".Type").equalsIgnoreCase(
                    "Random")) {
                doRandomEffects(p);
            }
        }

        int itemi = p.getItemInHand().getTypeId();
        short dura = p.getItemInHand().getDurability();
        String ond = "";

        if (dura <= 0) {
            ond = plugin.config.getString("chat.broadcast.OnDrugs").replaceAll(
                    "%drugname%",
                    plugin.config.getString("DrugIds." + itemi + ".DrugName"));
        } else {
            ond = plugin.config.getString("chat.broadcast.OnDrugs").replaceAll(
                    "%drugname%",
                    plugin.config.getString("DrugIds." + itemi + ":" + dura
                    + ".DrugName"));
        }

        ond = ond.replaceAll("%playername%", p.getName());
        p.sendMessage(plugin.colorize(ond));

        plugin.getEffectList().clear();
    }

    public void doRandomEffects(Player p) {
        int ii = plugin.getEffectList().size();
        int iRandom = gen.nextInt(ii);

        if (iRandom < 0) {
            iRandom = 0;
        }
        if (iRandom > ii) {
            iRandom = ii;
        }

        int x = plugin.getEffectList().get(iRandom);

        applyEffect(p, x);
    }

    public void applyEffect(Player p, int i) {

        plugin.getOnDrugs().add(p.getName());

        // All potion effects here:
        // http://www.minecraftwiki.net/wiki/Status_effect

        if (i == 0) {
            // Portal Effect
            walkWeird(p);
        } else if (i == 1) {
            // Zoom-In & Walk Slow
            walkSlow(p);
        } else if (i == 2) {
            // Zoom-Out & Walk Fast
            walkFast(p);
        } else if (i == 3) {
            // Blind
            blindMe(p);
        } else if (i == 4) {
            // Hunger
            soHungry(p);
        } else if (i == 5) {
            // High Jump
            feelingJumpy(p);
        } else if (i == 6) {
            // Sickness & Slower Hitting
            soSick(p);
        } else if (i == 7) {
            // Drunk
            drunk(p);
        }
    }

    public String scramble(String word) {
        StringBuilder builder = new StringBuilder(word.length());
        boolean[] used = new boolean[word.length()];

        for (int iScramble = 0; iScramble < word.length(); iScramble++) {
            int rndIndex;
            do {
                rndIndex = new Random().nextInt(word.length());
            } while (used[rndIndex] != false);
            used[rndIndex] = true;

            builder.append(word.charAt(rndIndex));
        }
        return builder.toString();
    }

    public void doSlow(Player p) {
        int speed = 5;
        int ran = this.gen.nextInt(3);
        if (ran != 2) {
            int rblock = this.gen.nextInt(4);
            Block b = null;
            if (rblock == 0) {
                b = p.getLocation().getBlock()
                        .getRelative(BlockFace.NORTH, speed);
            } else if (rblock == 1) {
                b = p.getLocation().getBlock()
                        .getRelative(BlockFace.SOUTH, speed);
            } else if (rblock == 2) {
                b = p.getLocation().getBlock()
                        .getRelative(BlockFace.EAST, speed);
            } else if (rblock == 3) {
                b = p.getLocation().getBlock()
                        .getRelative(BlockFace.WEST, speed);
            } else {
                b = p.getLocation().getBlock()
                        .getRelative(BlockFace.SELF, speed);
            }
            double val = 0.1D;
            Vector v = new Vector(b.getLocation().getX() * val, 0.0D, 0.0D);
            p.setVelocity(v);
        }
    }

    public void walkWeird(Player p) {

        int power = gen.nextInt(100);
        if (power <= 10) {
            power = 10;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, ran,
                power));
    }

    public void walkSlow(final Player p) {
        int power = gen.nextInt(100);
        if (power <= 10) {
            power = 10;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ran, power));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
            }
        }, ran);
    }

    public void walkFast(final Player p) {
        int power = gen.nextInt(100);
        if (power <= 10) {
            power = 10;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ran, power));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
            }
        }, ran);
    }

    public void blindMe(final Player p) {
        int power = gen.nextInt(1000);
        if (power <= 100) {
            power = 100;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ran,
                power));
        p.canSee(p);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
            }
        }, ran);
    }

    public void soHungry(final Player p) {
        final int food = p.getFoodLevel();

        int power = gen.nextInt(1000);
        if (power <= 100) {
            power = 100;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, ran, power));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
                p.setFoodLevel(food / 2);
            }
        }, ran);
    }

    public void soSick(final Player p) {
        int power = gen.nextInt(1000);
        if (power <= 100) {
            power = 100;
        }
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }

        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, ran,
                power));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ran,
                power));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
            }
        }, ran);
    }

    public void feelingJumpy(final Player p) {
        plugin.getOnDrugs().add(p.getName());

        int power = gen.nextInt(15);
        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ran, power));

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getOnDrugs().remove(p.getName());
                plugin.getIsJump().remove(p.getName());
            }
        }, ran);
    }

    @SuppressWarnings("deprecation")
    public void pukeInv(Player p) {
        int itemi = p.getItemInHand().getTypeId();
        short dura = p.getItemInHand().getDurability();
        String puke = "";

        if (plugin.config.getBoolean("Options.EnableEffectMessages") == true) {
            if (dura == 0) {
                puke = plugin.config.getString("chat.broadcast.Puke")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ".DrugName"));
            } else {
                puke = plugin.config.getString("chat.broadcast.Puke")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ":" + dura + ".DrugName"));
            }
            puke = puke.replaceAll("%playername%", p.getName());
            Bukkit.broadcastMessage(plugin.colorize(puke));
        }
        ItemStack[] inventory = p.getInventory().getContents();
        Location l = p.getLocation().getBlock().getRelative(BlockFace.NORTH, 3)
                .getLocation();
        p.getInventory().clear();
        for (ItemStack item : inventory) {
            if (item != null) {
                p.getWorld().dropItemNaturally(l, item);
                p.updateInventory();
            }
        }
        p.updateInventory();
    }

    public void torchYa(Player p) {
        int itemi = p.getItemInHand().getTypeId();
        short dura = p.getItemInHand().getDurability();
        String hot = "";

        if (plugin.config.getBoolean("Options.EnableEffectMessages") == true) {
            if (dura == 0) {
                hot = plugin.config.getString("chat.broadcast.Burning")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ".DrugName"));
            } else {
                hot = plugin.config.getString("chat.broadcast.Burning")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ":" + dura + ".DrugName"));
            }
            hot = hot.replaceAll("%playername%", p.getName());
            Bukkit.broadcastMessage(plugin.colorize(hot));
        }
        p.setFireTicks(200);
    }

    public void youOd(Player p) {
        int itemi = p.getItemInHand().getTypeId();
        short dura = p.getItemInHand().getDurability();
        String death = "";

        if (plugin.config.getBoolean("Options.EnableEffectMessages") == true) {
            if (dura == 0) {
                death = plugin.config.getString("chat.broadcast.Death")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ".DrugName"));
            } else {
                death = plugin.config.getString("chat.broadcast.Death")
                        .replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ":" + dura + ".DrugName"));
            }
            death = death.replaceAll("%playername%", p.getName());
            Bukkit.broadcastMessage(plugin.colorize(death));
        }
        p.setHealth(0);
    }

    public void heartAttack(final Player p) {
        int itemi = p.getItemInHand().getTypeId();
        short dura = p.getItemInHand().getDurability();
        String heartAttack;

        if (plugin.config.getBoolean("Options.EnableEffectMessages") == true) {
            if (dura == 0) {
                heartAttack = plugin.config.getString(
                        "chat.broadcast.HeartAttack").replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi
                        + ".DrugName"));
            } else {
                heartAttack = plugin.config.getString(
                        "chat.broadcast.HeartAttack").replaceAll(
                        "%drugname%",
                        plugin.config.getString("DrugIds." + itemi + ":" + dura
                        + ".DrugName"));
            }
            heartAttack = heartAttack.replaceAll("%playername%", p.getName());
            Bukkit.broadcastMessage(plugin.colorize(heartAttack));
        }
        plugin.getHeartAttack().add(p.getName());

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getHeartAttack().remove(p.getName());
            }
        }, 100L);

        if (p.getHealth() >= 2) {
            p.setHealth(p.getHealth() - 1);
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.getHeartAttack().contains(p.getName())) {
                    if (i == 0) {
                        if (p.getHealth() >= 2) {
                            p.setHealth(p.getHealth() - 1);
                        }
                        i = 1;
                    } else {
                        p.setHealth(p.getHealth() + 1);
                        i = 0;
                    }
                }
            }
        }, 0L, 3L);
    }

    public void drunk(final Player p) {
        plugin.getDrunk().add(p.getName());

        int ran = gen.nextInt(1000);
        if (ran <= 300) {
            ran = 300;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                p.sendMessage(plugin.colorize(plugin.config
                        .getString("chat.broadcast.Sober")));
                plugin.getDrunk().remove(p.getName());
            }
        }, ran);
    }
}
