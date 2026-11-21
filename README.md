# NajlepszeServeryMC Rewards

Oficjalny, open-source plugin Paper nagród za głosy na [najlepszeserverymc.pl](https://najlepszeserverymc.pl).

Plugin odpytuje API strony, a gdy gracz z podanym nickiem jest online, wykonuje komendy z `config.yml` i oznacza głos jako odebrany.

## Wymagania

- Paper / Purpur **1.20+** (budowane pod Java 21)
- Serwer dodany na [najlepszeserverymc.pl](https://najlepszeserverymc.pl)
- Token z panelu: **Edytuj serwer → Plugin nagród**

## Instalacja

1. Pobierz JAR z [Releases](https://github.com/Muszek1/najlepszeserverymc-rewards/releases) albo zbuduj lokalnie.
2. Wrzuć do `plugins/` i zrestartuj serwer.
3. W panelu strony wygeneruj token.
4. W `plugins/NajlepszeServeryMC-Rewards/config.yml` ustaw:

```yaml
api-url: "https://najlepszeserverymc.pl/api/v1/plugin"
token: "nsmc_...."
poll-interval-seconds: 15
commands:
  - "say %player% zagłosował na najlepszeserverymc.pl!"
  # - "give %player% diamond 1"
```

5. `/nsmcrewards reload`

## Komendy

| Komenda | Opis |
|---------|------|
| `/nsmcrewards reload` | Przeładuj config |
| `/nsmcrewards status` | Status połączenia |
| `/nsmcrewards poll` | Ręczny poll API |
| `/nsmcrewards test [nick]` | Test komend nagród |

Uprawnienie: `nsmcrewards.admin` (domyślnie OP).

## API (strona)

- `GET /api/v1/plugin/votes/pending` — `Authorization: Bearer <token>`
- `POST /api/v1/plugin/votes/claim` — body `{ "ids": ["..."] }`

## Build

```bash
mvn -B package
```

JAR: `target/NajlepszeServeryMC-Rewards-1.0.0.jar`

## Licencja

MIT
