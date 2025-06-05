package ru.finwax.mangabuffjob.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MangaReadingProgressId implements Serializable {
    private MangaData manga;
    private UserCookie userCookie;
} 