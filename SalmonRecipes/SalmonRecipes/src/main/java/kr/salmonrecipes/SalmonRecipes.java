package kr.salmonrecipes;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SalmonRecipes extends JavaPlugin implements Listener {

    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private String allowedPlayer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        allowedPlayer = getConfig().getString("allowed-player", "salmon4492");
        getServer().getPluginManager().registerEvents(this, this);

        String myName = getConfig().getString("my-head-owner", "salmon4492");
        String friendName = getConfig().getString("friend-head-owner", "xxthemadxx");

        // 머리 스킨 정보는 인터넷에서 가져와야 하므로 비동기로 처리한 뒤, 메인 스레드에서 조합법 등록
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            PlayerProfile myProfile = loadProfile(myName);
            PlayerProfile friendProfile = loadProfile(friendName);

            Bukkit.getScheduler().runTask(this, () -> {
                registerRecipes(makeHead(myProfile), makeHead(friendProfile));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    hideFromRecipeBook(online);
                }
                getLogger().info("커스텀 조합법 4개 등록 완료");
            });
        });
    }

    // ---------- 사용 권한 / 조합법 책 숨김 ----------

    private boolean isOurs(Recipe recipe) {
        return recipe instanceof ShapedRecipe shaped && recipeKeys.contains(shaped.getKey());
    }

    private boolean isAllowed(HumanEntity player) {
        return player.getName().equalsIgnoreCase(allowedPlayer);
    }

    private void hideFromRecipeBook(Player player) {
        player.undiscoverRecipes(recipeKeys);
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe != null && isOurs(recipe) && !isAllowed(event.getView().getPlayer())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (isOurs(event.getRecipe()) && !isAllowed(event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        hideFromRecipeBook(event.getPlayer());
    }

    // ---------- 조합법 등록 ----------

    private PlayerProfile loadProfile(String name) {
        PlayerProfile profile = Bukkit.createProfile(name);
        try {
            profile.complete(true); // 스킨 텍스처까지 조회
        } catch (Exception e) {
            getLogger().warning(name + " 스킨 조회 실패 (이름만 설정됩니다): " + e.getMessage());
        }
        return profile;
    }

    private ItemStack makeHead(PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    private void add(ShapedRecipe recipe) {
        Bukkit.addRecipe(recipe);
        recipeKeys.add(recipe.getKey());
    }

    private void registerRecipes(ItemStack myHead, ItemStack friendHead) {
        // 1) 흙 8개 + 가운데 슬랩 -> 내 머리
        ShapedRecipe r1 = new ShapedRecipe(new NamespacedKey(this, "my_head"), myHead);
        r1.shape("DDD", "DSD", "DDD");
        r1.setIngredient('D', Material.DIRT);
        r1.setIngredient('S', new RecipeChoice.MaterialChoice(Tag.WOODEN_SLABS));
        add(r1);

        // 2) 흙 8개 + 가운데 나무 판자 -> 친구 머리
        ShapedRecipe r2 = new ShapedRecipe(new NamespacedKey(this, "friend_head"), friendHead);
        r2.shape("DDD", "DPD", "DDD");
        r2.setIngredient('D', Material.DIRT);
        r2.setIngredient('P', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        add(r2);

        // 3) 돌 8개 + 가운데 다이아몬드 -> 다이아몬드 광석
        ShapedRecipe r3 = new ShapedRecipe(new NamespacedKey(this, "diamond_ore"), new ItemStack(Material.DIAMOND_ORE));
        r3.shape("SSS", "SDS", "SSS");
        r3.setIngredient('S', Material.STONE);
        r3.setIngredient('D', Material.DIAMOND);
        add(r3);

        // 4) 모서리 다이아 4개 + 상하좌우 돌 4개 + 가운데 재료 -> 무거운 코어
        Material center = Material.matchMaterial(getConfig().getString("heavy-core-center", "SUGAR"));
        if (center == null) {
            getLogger().warning("heavy-core-center 값이 올바르지 않아 SUGAR로 대체합니다.");
            center = Material.SUGAR;
        }
        ShapedRecipe r4 = new ShapedRecipe(new NamespacedKey(this, "heavy_core"), new ItemStack(Material.HEAVY_CORE));
        r4.shape("DSD", "SXS", "DSD");
        r4.setIngredient('D', Material.DIAMOND);
        r4.setIngredient('S', Material.STONE);
        r4.setIngredient('X', center);
        add(r4);
    }
}
