import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { JobSearchComponent } from './job-search.component';
import { JobPosting } from './job-posting.model';

describe('JobSearchComponent', () => {
  let fixture: ComponentFixture<JobSearchComponent>;
  let component: JobSearchComponent;
  let httpMock: HttpTestingController;

  const job: JobPosting = {
    id: '11111111-1111-1111-1111-111111111111',
    title: 'Automotive QA Engineer',
    description: 'desc',
    sector: 'automotive',
    city: 'Tangier',
    contractType: 'CDI',
    status: 'LIVE',
    companyName: 'Atlas Automotive SARL',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [JobSearchComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(JobSearchComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads jobs on init', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/jobs').flush([job]);

    expect(component.jobs()).toEqual([job]);
    expect(component.loading()).toBe(false);
  });

  it('sends filter params on search', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/jobs').flush([]);

    component.filters.setValue({ sector: 'automotive', city: 'Tangier', contractType: 'CDI' });
    component.search();

    const req = httpMock.expectOne(
      (r) => r.url === '/api/v1/jobs' && r.params.get('sector') === 'automotive' && r.params.get('city') === 'Tangier'
    );
    req.flush([job]);
    expect(component.jobs()).toEqual([job]);
  });

  it('surfaces a search error', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/jobs').flush('boom', { status: 500, statusText: 'Server Error' });

    expect(component.error()).toBe('Search failed, retry');
    expect(component.loading()).toBe(false);
  });
});
