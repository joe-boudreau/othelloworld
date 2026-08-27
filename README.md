# othelloworld
artisanal othello "AI" engine

https://flowtwo.io/othelloworld/


make selfplay NUM_GAMES=5000    # Kotlin plays games, writes CSVs to data/
make train ALPHA=1.0            # Python fits Ridge, logs run to MLflow, writes weights_latest.json
make mlflow-ui                  # browse localhost:5000, compare this run to past ones
make benchmark                  # play weights_latest vs weights_v1, check win rate
