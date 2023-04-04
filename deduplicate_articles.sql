-- Find duplicates in the Weltladenkasse via MySQL:
-- (This query should return nothing if there are no duplicates.)
-- (Duplicate means: two *active* articles having the same lieferant_id and the same case-insensitive article number.)
SELECT * FROM (SELECT lieferant_id, artikel_nr, COUNT(*) AS count FROM artikel WHERE aktiv = TRUE GROUP BY lieferant_id, artikel_nr) AS t WHERE t.count > 1;

-- Compare the duplicates:
-- (Look if it's OK to keep only the most recent, probably most relevant article, i.e. with highest artikel_id.)
SELECT artikel.lieferant_id, artikel.artikel_nr, artikel.artikel_id, artikel.von, artikel.vk_preis, artikel.barcode FROM artikel JOIN (SELECT * FROM (SELECT lieferant_id, artikel_nr, COUNT(*) AS count FROM artikel WHERE aktiv = TRUE GROUP BY lieferant_id, artikel_nr) AS counts WHERE counts.count > 1) AS numbers ON artikel.lieferant_id = numbers.lieferant_id AND artikel.artikel_nr = numbers.artikel_nr ORDER BY numbers.lieferant_id, numbers.artikel_nr, artikel.artikel_id;

-- Select the older, more irrelevant articles of the duplicates:
SELECT MIN(artikel.artikel_id) FROM artikel INNER JOIN (SELECT * FROM (SELECT lieferant_id, artikel_nr, COUNT(*) AS count FROM artikel WHERE aktiv = TRUE GROUP BY lieferant_id, artikel_nr) AS counts WHERE counts.count > 1) AS numbers ON artikel.lieferant_id = numbers.lieferant_id AND artikel.artikel_nr = numbers.artikel_nr GROUP BY artikel.lieferant_id, artikel.artikel_nr;

-- Remove the redundant duplicate articles from the DB (set them to inactive and set 'bis' date to now)
UPDATE artikel INNER JOIN (SELECT MIN(artikel.artikel_id) AS artikel_id FROM artikel INNER JOIN (SELECT * FROM (SELECT lieferant_id, artikel_nr, COUNT(*) AS count FROM artikel WHERE aktiv = TRUE GROUP BY lieferant_id, artikel_nr) AS counts WHERE counts.count > 1) AS numbers ON artikel.lieferant_id = numbers.lieferant_id AND artikel.artikel_nr = numbers.artikel_nr GROUP BY artikel.lieferant_id, artikel.artikel_nr) AS a ON artikel.artikel_id = a.artikel_id SET artikel.aktiv = 0, artikel.bis = NOW();

-- Check whether it has worked (with one of the affected articles):
SELECT lieferant_id, artikel_nr, artikel_id, vk_preis, barcode, von, bis, aktiv FROM artikel WHERE artikel_nr = 'AE2-22-126';