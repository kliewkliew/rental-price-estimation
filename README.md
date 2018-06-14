# Rental Price Estimation
This project scrapes data from Craigslist and uses it to estimate rental prices for apartments based on:
- the number of bedrooms 
- square footage
- neighborhood the apartment is located in.

## Usage
Note that scraping and then estimating a different city is not currently supported because the data is not separated from other city data. To estimate for different cities, scrape for that city and reset the database, retrain the model, and then do the estimation.

### Build
```
sh ./build
```
You need Java 8 to build the application.

### Scrape
- sh ./scrape
  - reset_db
  - url: defaults to https://sfbay.craigslist.org/search/apa

ie.
```
sh ./scrape -r -u https://vancouver.craigslist.ca/search/apa
```

### Train
```
sh ./model_train
```

### Estimate
- sh ./model_estimate
  - bedrooms
  - square_feet
  - neighborhood

ie.
```
sh ./model_estimate -n "Downtown" -s 570 -b 1
```
The neighborhood name must exactly match a neighborhood in the city that was scraped.

## Implementation

### Scraper
Note that using the scraper too frequently may result in a temporary ban from Craigslist.

#### Data Storage
SQLite is used for persisting scraped listings.
```
create table apartment_listings (
    listing_id integer,
    listing_price integer,
    bedrooms integer,
    square_feet integer,
    neighborhood text
)
```

### Model Training and Estimation
The model is trained using gradient boosting with linear base-learners.
The neighborhood names are not normalized, including the input to the command line application.
Neighborhood strings input to the model with one-hot encoding scheme.

## Possible Improvements
* invest more time in feature-engineering the learning model
* filter fake and outdated listings
* additional methods for scraping missing information from the page of the listing if not present in the search results page
* account for different locales including currency and metric systems
* improve code quality by using parameterized SQL statements
* modify sqlite to use a primary key on the listing id so that multi-row insert statements can be done and multi-tenancy is supported.
* do normalization of neighborhoods (including prediction input nbh) by using Google Maps API to get a more accurate neighborhood name and mapping that to a zip code or GPS coordinates for the neighborhood center
* scrape from additional data sources. ie. Zillow, Apartment List
