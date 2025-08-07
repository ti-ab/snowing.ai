/* ────────────────────────────────────────────────────────────────
   Schéma « course_library » : livre de cours, chapitres, etc.
   ──────────────────────────────────────────────────────────────── */
create schema if not exists courses;
set search_path to courses;

/* ───── Livres ────────────────────────────────────────────────── */
create table books (
    id          bigserial      primary key,
    title       varchar(255)   ,
    authors     varchar(255),
    created_at  timestamptz     default now()
);

/* ───── Chapitres ─────────────────────────────────────────────── */
create table chapters (
    id          bigserial      primary key,
    book_id     bigint          references books(id) on delete cascade,
    idx         int            ,            -- ordre dans le livre
    title       varchar(255)   ,
    unique(book_id, idx)                          -- 1..8 par livre
);

/* ───── Sous‑chapitres ────────────────────────────────────────── */
create table subchapters (
    id          bigserial      primary key,
    chapter_id  bigint          references chapters(id) on delete cascade,
    idx         int            ,            -- ordre dans le chapitre
    title       varchar(255)   ,
    unique(chapter_id, idx)                       -- 1..4 par chapitre
);

/* ───── Sections ──────────────────────────────────────────────── */
create table sections (
    id               bigserial primary key,
    subchapter_id    bigint     references subchapters(id) on delete cascade,
    idx              int       ,            -- ordre dans le sous‑chapitre
    title            varchar(255) ,
    section_summary  text,
    first_paragraph  text,
    middle_paragraph text,
    end_paragraph    text,
    content          text,
    unique(subchapter_id, idx)                    -- 1..4 par sous‑chapitre
);

/* ───── Indexes utiles (recherche & jointures) ────────────────── */
create index idx_chapters_book  on chapters(book_id);
create index idx_subchap_chap   on subchapters(chapter_id);
create index idx_sections_subc on sections(subchapter_id);

/* ───── (Option) vues pour requêtes fréquentes ───────────────── */
-- Exemple : vue plate « sections_full » regroupant tout le contexte
create or replace view sections_full as
select  b.id          as book_id,
        b.title       as book_title,
        c.idx         as chapter_idx,
        c.title       as chapter_title,
        sc.idx        as subchapter_idx,
        sc.title      as subchapter_title,
        s.idx         as section_idx,
        s.title       as section_title,
        s.content
from books b
join chapters    c  on c.book_id     = b.id
join subchapters sc on sc.chapter_id = c.id
join sections    s  on s.subchapter_id = sc.id
order by b.id, c.idx, sc.idx, s.idx;
