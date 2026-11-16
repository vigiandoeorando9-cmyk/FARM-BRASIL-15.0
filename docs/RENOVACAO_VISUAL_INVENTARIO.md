# Farm Brasil — Renovação visual: inventário e plano seguro

**Escopo deste registro:** planejamento de Etapas 2–3 e inventário inicial baseado no código-fonte. Este arquivo não representa aprovação visual, implementação de modelos, nem resultado de build/teste.  
**Snapshot inspecionado:** `main` em `33a5a2bcaaa38e0ef8775a9923ac83fb0e415b34` (16 nov 2026).  
**Ponto de restauração confirmado:** `backup/etapa-1-pre-jd6300`, no mesmo commit.  
**Branch de trabalho deste registro:** `visual/etapa-2-lote1-tratores-diagnostico`.

## Decisões de preservação

- Não alterar física, controles, colisões, save schema, economia, multiplayer nem IDs/catálogos para trabalho exclusivamente visual.
- Não substituir equipamento ausente por imagem genérica. Se não há representação/asset identificável, registrar PENDENTE e manter o visual atual enquanto se prepara conteúdo autorizado.
- Não prometer qualidade FS25/FS26 por mudança procedural no Compose Canvas. A arquitetura observada projeta/desenha primitivas em `GameplayViewport.kt`, `ViewportMath3D.kt` e renderers; não há dependência de engine 3D nem malhas de máquinas `.glb/.gltf/.obj/.fbx` na árvore do snapshot.
- Não tratar metadados de ModHub (tamanho/compatibilidade/descrição) como prova de arquivos instaláveis. `HubCatalog.kt` classifica parte dos pacotes gráficos como `EXTERNAL_FILE_REQUIRED` e WIP como bloqueado; `ModDownloadManager.kt` não finge instalar esses arquivos.
- `ANALISADO` significa ponto de entrada/família de código identificados, não que todas as definições foram inspecionadas uma a uma, nem que sua aparência foi validada.

## Arquitetura visual observada

| Área | Arquivos/pontos de entrada | Situação observada | Estado do trabalho visual |
|---|---|---|---|
| Gameplay/câmera/projeção | `ui/components/GameplayViewport.kt`, `ViewportMath3D.kt` | Canvas Compose, câmera em modos CABINE/ORBITAL/TOPO/LIVRE/TERCEIRA_PESSOA, projeção 3D e ordenação de render items | ANALISADO; PENDENTE de melhoria |
| Veículos e implementos da cena | `ui/components/ViewportVehicleRenderer.kt` | `drawVehicle3D`, `drawImplement3D`; veículos roteados principalmente por `VehicleBodyStyle`, implementos por `ImplementCategory`; primitivas com dimensões e cores | ANALISADO; PENDENTE de diferenciação visual por definição |
| Iluminação/materiais | `ui/components/ViewportLightingAndMaterials.kt`, `ViewportMath3D.kt` | Tipos de material e shading customizado (luz direcional, specular/Fresnel aproximado, AO/fog e sombra projetada); não é pipeline PBR de engine | ANALISADO; PENDENTE de avaliação visual em aparelho |
| Cabine/cockpit | `ViewportCabOverlay.kt`, bloco CABINE de `ViewportVehicleRenderer.kt` | Overlay de cabine, limpador/chuva, mostradores e formas de painel | ANALISADO; PENDENTE de validação de proporções por modelo |
| Galeria/preview | `ui/components/MachineGalleryModal.kt`, `ui/components/FarmUiKit.kt` | Modal com ângulos e zoom; previews desenhados por código | ANALISADO; pendente localizar e classificar a função de preview em `FarmUiKit.kt` |
| Tratores e paletas | `domain/TractorCatalogPart1.kt`, `TractorCatalogPart2.kt`, `HistoricalCatalog.kt`, `BrandPalette.kt` | Definições compartilham classes de carroceria/perfis; palette por fabricante, com exceções por alguns IDs | ANALISADO por família; fidelidade de cada modelo NÃO ANALISADA |
| Colheitadeiras e outros veículos | `domain/MachineCatalog.kt`, `VehicleExpansionCatalog.kt`, `HistoricalCatalog.kt`, `ModExpansionCatalog.kt` | Catálogos de definições e agregação em `ALL_VEHICLES`; representação de colheitadeira segue estilo de carroceria | ANALISADO por família; PENDENTE por modelo |
| Implementos/plataformas | `domain/MachineCatalog.kt`, `ImplementExpansionCatalog.kt`, `ModExpansionCatalog.kt`; `DrivetrainPhysics.kt` | Implementos têm categoria, hitch, largura, peso e capacidade; renderização por categoria. `PLATAFORMA_MILHO` usa hitch `HEADER`; compatibilidade mecânica é checada em `checkMachineImplementCompatibility` | ANALISADO por família; mapa de compatibilidade/largura e aparência individual PENDENTES |
| Caminhões/transporte/linha amarela | `VehicleExpansionCatalog.kt`, `MachineCatalog.kt`, `ModExpansionCatalog.kt`, renderer de veículos | Uma função de desenho simples é usada para alguns veículos de tráfego/contratados; veículos próprios seguem corpo por estilo/categoria | ANALISADO; PENDENTE de auditoria de cada tipo/modelo |
| Construções | `domain/BuildingCatalog.kt`, `ui/components/ViewportBuildingRenderer.kt` | Catálogo de estruturas/perfis regionais e renderer com formas procedurais | ANALISADO; PENDENTE de validação por estrutura |
| Terreno/mapas | `domain/MapCatalog.kt`, `simulation/TerrainGenerator.kt`, `ui/components/ViewportWorldRenderer.kt` | Mapas e terreno procedurais por arquétipo/bioma; grade LOD e estados de solo desenhados no Canvas | ANALISADO; PENDENTE de inventário visual por bioma |
| Culturas/árvores/props | `domain/CropCatalog.kt`, `Models.kt`, `TerrainGenerator.kt`, `ViewportWorldRenderer.kt` | Catálogos e tipos de props; culturas desenhadas com hastes/traços/círculos e árvores com troncos/volumes geométricos | ANALISADO por renderer; diversidade/modelos por espécie PENDENTES |
| Água/terraplanagem/fauna aquática | `WaterAndAquacultureModels.kt`, `EarthworkWaterAndPiscicultureCatalog.kt`, `ViewportWorldRenderer.kt`, `ViewportBuildingRenderer.kt` | Estados de água, açude, peixes e marcas de terraplanagem; desenho programático | ANALISADO; PENDENTE por asset/espécie/elemento |
| Mods | `domain/HubCatalog.kt`, `ModExpansionCatalog.kt`, `storage/ModDownloadManager.kt` | Mods integrados no código convivem com pacotes que exigem arquivo externo e pacotes WIP bloqueados | ANALISADO; inventário de cada pack/asset/licença PENDENTE |
| Assets binários no snapshot | `app/src/main/res/drawable`, `app/src/main/res/font`, `public/assets/aistudio` | Imagens promocionais/mapa/ícone e fontes; seis imagens em `public/assets/aistudio`; nenhuma malha 3D identificada na árvore | ANALISADO por extensão/árvore; uso/licença de cada imagem PENDENTE |
| Otimização | `simulation/WorldStreamingEngine.kt`, `GameplayViewport.kt`, `ViewportWorldRenderer.kt` | Chunks, LOD0/1/2, frustum/distance culling, limites móveis de chunks/props e scratch buffers | ANALISADO em código; custo em dispositivos reais NÃO TESTADO |
| Plataformas | `app/build.gradle.kts`, `AndroidManifest.xml`, `.github/workflows/build.yml` | Android configurado e workflow publica APK debug. Perfil “Desktop/PC” não equivale a build Windows | Android ANALISADO; Windows sem target/build comprovado |
| Física/saves/controle preservados | `simulation/VehiclePhysics.kt`, `DrivetrainPhysics.kt`, `WorldCollision.kt`, `state/FarmViewModel.kt`, `storage/CareerStorage.kt` | Sistemas em módulos separados do desenho da cena | ANALISADO em alto nível; nenhum teste executado nesta etapa |

## Contagens: fonte e ressalvas

`domain/FeatureRegistry.kt` declara **457 máquinas** (353 tratores, 38 colheitadeiras, 35 caminhões e 31 máquinas especiais/linha amarela), **145 implementos** e **55 culturas**. São contagens declaradas pelo registry, não uma contagem independente executada neste inventário. O ModHub declara muitos packs, mas packs integrados, externos e WIP têm estados de entrega diferentes. Na próxima revisão quantitativa, enumerar IDs diretamente dos catálogos e distinguir: definição de jogo, entidade renderizável, preview, asset binário, dependência externa e autorização/licença.

## Registro por categoria (estado inicial)

Legenda permitida: NÃO ANALISADO, ANALISADO, PENDENTE, EM DESENVOLVIMENTO, IMPLEMENTADO, TESTADO, APROVADO. Um mesmo item pode ter status de inventário ANALISADO e status de renovação PENDENTE.

| Lote | Cobertura | Fontes principais | Inventário | Renovação | Verificação visual |
|---|---|---|---|---|---|
| 1 Tratores | Todas as marcas/linhas, cabine e plataformas abertas | `TractorCatalogPart1.kt`, `TractorCatalogPart2.kt`, `HistoricalCatalog.kt`, `ModExpansionCatalog.kt`, `BrandPalette.kt`, `ViewportVehicleRenderer.kt` | ANALISADO por família; IDs individuais pendentes de enumeração | PENDENTE | NÃO ANALISADO |
| 2 Colheitadeiras | Todas as classes, rodas/esteiras e descarga | `MachineCatalog.kt`, `VehicleExpansionCatalog.kt`, `HistoricalCatalog.kt`, `ModExpansionCatalog.kt`, `ViewportVehicleRenderer.kt` | ANALISADO por família; IDs individuais pendentes de enumeração | PENDENTE | NÃO ANALISADO |
| 3 Plataformas | Corte, milho, headers e transporte de plataforma | `MachineCatalog.kt`, `ImplementExpansionCatalog.kt`, `ModExpansionCatalog.kt`, `DrivetrainPhysics.kt`, `ViewportVehicleRenderer.kt` | ANALISADO por família; compatibilidade completa pendente de matriz verificável | PENDENTE | NÃO ANALISADO |
| 4 Plantadeiras/semeadoras | Linhas, dosagem, discos, reservatórios e dobragem | `MachineCatalog.kt`, `ImplementExpansionCatalog.kt`, `ModExpansionCatalog.kt`, `ViewportVehicleRenderer.kt` | ANALISADO por família | PENDENTE | NÃO ANALISADO |
| 5 Pulverizadores/distribuidores | Tanques, barras, bicos e discos | mesmos catálogos de implementos; `ViewportParticles.kt` | ANALISADO por família | PENDENTE | NÃO ANALISADO |
| 6 Outros implementos | Arados, grades, subsoladores, escarificadores, feno/silagem e acessórios | `MachineCatalog.kt`, `ImplementExpansionCatalog.kt`, `ModExpansionCatalog.kt`, renderer | ANALISADO por família | PENDENTE | NÃO ANALISADO |
| 7 Caminhões/carretas | Transporte agrícola e rodoviário, carrocerias e combinações | catálogos de veículos/implementos e renderer | ANALISADO por família | PENDENTE | NÃO ANALISADO |
| 8 Peças/acessórios | Rodas, pneus, aros, escapamento, cabine, luzes, pintura | `MachineCatalog.kt`, `Models.kt`, `BrandPalette.kt`, `ViewportVehicleRenderer.kt` | ANALISADO por família | PENDENTE | NÃO ANALISADO |
| 9 Mods restantes | Packs integrados, importados, externos e bloqueados/WIP | `HubCatalog.kt`, `ModExpansionCatalog.kt`, `ModDownloadManager.kt` | ANALISADO por entrega; auditoria individual/licenças pendente | PENDENTE | NÃO ANALISADO |

## Critérios de progresso

- **IMPLEMENTADO:** mudanças existem no branch e afetam o item identificado, sem alegar fidelidade não verificada.
- **TESTADO:** build/teste automatizado aplicável executado com resultado registrado; compatibilidade de runtime ainda requer dispositivo quando relevante.
- **APROVADO:** revisão visual e funcional registrada em dispositivos/configurações alvo, com referência, versão e evidência.
- Reutilizar geometria comum é permitido para componentes mecânicos compartilhados, mas não como substituto de silhueta/identidade do modelo. Se um equipamento não tem referência/licença/asset adequado, deixar PENDENTE, sem rotulá-lo como renovado.
- Para plataforma, verificar tipo HEADER, categoria de colheitadeira, largura de corte e capacidade/compatibilidade mecânica; impedir combinação impossível. Não alterar regras físicas sem tarefa e testes próprios.
- Para mods, verificar entrega real e permissão/licença antes de editar conteúdo de terceiros. Conteúdo externo ausente permanece marcado como pendente.

## Plano de execução seguro

### Preparação — antes de qualquer código visual
1. Trabalhar somente em branch de tarefa; preservar `main` e o ponto `backup/etapa-1-pre-jd6300`.
2. Enumerar IDs e quantidades reais de cada catálogo, anexando ao ledger: ID, marca/modelo, categoria, renderer atual, tipo de preview, asset disponível, fonte/licença e estado.
3. Confirmar baseline de build/testes em ambiente de desenvolvimento, sem instalar dependências nem reconstruir projeto fora da tarefa autorizada. Guardar resultado e limitações.
4. Inspecionar renderer de preview da galeria em `FarmUiKit.kt`; mapear se ele compartilha ou diverge do renderer de gameplay.
5. Estabelecer critérios de imagem e desempenho mensuráveis por plataforma/dispositivo. Perfil PC existente não prova suporte Windows; investigar Windows separadamente.

### Lotes visuais (não executar em massa)
1. Tratores: melhorar o renderer compartilhado apenas com base de dados por modelo que suporte proporções e silhuetas específicas; verificar todas as marcas afetadas pelo renderer e customização. Não aprovar por paleta apenas.
2. Colheitadeiras: identidade e proporções por modelo, rodas/esteiras, cabine e descarga.
3. Plataformas: definir e testar a matriz de compatibilidade antes da UI de seleção; renderizar cada plataforma com largura/acoplamento corretos.
4. Plantadeiras e semeadoras.
5. Pulverizadores e distribuidores.
6. Implementos de preparo/feno/silagem e auxiliares.
7. Caminhões, carretas e transporte.
8. Rodas, pneus e acessórios, verificando os tipos e opções de customização existentes.
9. Mods: por pack e estado de entrega/licença; externos sem arquivo válido ficam pendentes.

Após cada lote: build Android, testes relevantes, regressão de renderer entre presets/câmeras, validação de itens/engates e comparação de capturas; atualizar os estados e registrar falhas. Não modificar física/save para melhoria visual. Se a abordagem exigir migração da pipeline Canvas ou alterações de alto risco, pausar e apresentar alternativas, riscos, custo e plano de compatibilidade antes.

### Encerramento da Etapa 2
Não declarar “todas as máquinas renovadas” até cada ID enumerado ter status de renovação e verificação. Relatar contagens por estado (aprovado, pendente, não testado) a partir do ledger, sem converter descrição comercial ou alteração de código em aprovação visual.

## Resultado desta preparação

- Arquivo de acompanhamento criado no branch de trabalho; nenhum código de jogo alterado.
- Nenhuma máquina/implemento foi renovado ou aprovado por este documento.
- Build, testes e inspeção visual em dispositivo: NÃO EXECUTADOS nesta preparação.
- Próxima ação é concluir a enumeração quantitativa por ID e baseline executável antes do primeiro lote de implementação.
