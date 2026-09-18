package com.example.scm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.scm.domain.Item;
import com.example.scm.domain.ItemSearchCriteria;

/**
 * 商品マスタ Mapper ―― 【アノテーション方式】の例。
 *
 * <p>学習ポイント:
 * <ul>
 *   <li>{@code #{...}} は PreparedStatement のプレースホルダ(?)になる。値は必ずこちらを使う。</li>
 *   <li>{@code ${...}} は文字列をそのまま SQL に埋め込む。SQLインジェクションの原因になるので、
 *       ORDER BY のカラム名など「値ではない箇所」に、かつ入力を検証したうえでのみ使う。</li>
 *   <li>{@code @Options(useGeneratedKeys = true, keyProperty = "id")} で
 *       INSERT 後に採番された ID が引数オブジェクトへ書き戻される。</li>
 *   <li>引数が2つ以上あるときは {@code @Param} で名前を付ける(付けないと arg0/param1 になる)。</li>
 *   <li>アノテーションに {@code <script>} を書くと動的SQLも使える。
 *       ただし量が増えたら XML (StockMapper.xml / ShipmentMapper.xml) に寄せたほうが読みやすい。</li>
 * </ul>
 */
@Mapper
public interface ItemMapper {

    String COLUMNS = "id, item_code, item_name, category, unit_price, safety_stock, created_at";

    @Select("SELECT " + COLUMNS + " FROM item ORDER BY item_code")
    List<Item> findAll();

    @Select("SELECT " + COLUMNS + " FROM item WHERE id = #{id}")
    Item findById(Integer id);

    @Select("SELECT " + COLUMNS + " FROM item WHERE item_code = #{itemCode}")
    Item findByItemCode(String itemCode);

    @Select("SELECT DISTINCT category FROM item ORDER BY category")
    List<String> findCategories();

    /**
     * 検索条件つき一覧。&lt;script&gt; を使ったアノテーション版の動的SQL。
     *
     * <p>sort は {@code ${}} で埋め込むため、呼び出し側(ItemService)で
     * ホワイトリスト検証済みの値しか渡らないようにしている。
     */
    @Select("""
            <script>
            SELECT id, item_code, item_name, category, unit_price, safety_stock, created_at
            FROM item
            <where>
              <if test="criteria.keyword != null">
                AND (LOWER(item_name) LIKE '%' || LOWER(#{criteria.keyword}) || '%'
                  OR LOWER(item_code) LIKE '%' || LOWER(#{criteria.keyword}) || '%')
              </if>
              <if test="criteria.category != null">
                AND category = #{criteria.category}
              </if>
              <if test="criteria.minPrice != null">
                AND unit_price &gt;= #{criteria.minPrice}
              </if>
              <if test="criteria.maxPrice != null">
                AND unit_price &lt;= #{criteria.maxPrice}
              </if>
            </where>
            ORDER BY ${orderByColumn}
            </script>
            """)
    List<Item> search(@Param("criteria") ItemSearchCriteria criteria,
                      @Param("orderByColumn") String orderByColumn);

    @Insert("""
            INSERT INTO item (item_code, item_name, category, unit_price, safety_stock, created_at)
            VALUES (#{itemCode}, #{itemName}, #{category}, #{unitPrice}, #{safetyStock}, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Item item);

    @Update("""
            UPDATE item
               SET item_code = #{itemCode},
                   item_name = #{itemName},
                   category = #{category},
                   unit_price = #{unitPrice},
                   safety_stock = #{safetyStock}
             WHERE id = #{id}
            """)
    int update(Item item);

    @Delete("DELETE FROM item WHERE id = #{id}")
    int deleteById(Integer id);
}
